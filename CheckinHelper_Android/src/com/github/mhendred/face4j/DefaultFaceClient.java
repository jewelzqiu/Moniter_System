/*
 * Copyright (c) 2010 Marlon Hendred
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.mhendred.face4j;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.github.mhendred.face4j.exception.FaceClientException;
import com.github.mhendred.face4j.exception.FaceServerException;
import com.github.mhendred.face4j.model.Namespace;
import com.github.mhendred.face4j.model.Photo;
import com.github.mhendred.face4j.model.RemovedTag;
import com.github.mhendred.face4j.model.SavedTag;
import com.github.mhendred.face4j.model.UserStatus;
import com.github.mhendred.face4j.response.GetTagsResponse;
import com.github.mhendred.face4j.response.GetTagsResponseImpl;
import com.github.mhendred.face4j.response.GroupResponse;
import com.github.mhendred.face4j.response.GroupResponseImpl;
import com.github.mhendred.face4j.response.LimitsResponse;
import com.github.mhendred.face4j.response.LimitsResponseImpl;
import com.github.mhendred.face4j.response.NamespaceResponse;
import com.github.mhendred.face4j.response.NamespaceResponseImpl;
import com.github.mhendred.face4j.response.PhotoResponse;
import com.github.mhendred.face4j.response.PhotoResponseImpl;
import com.github.mhendred.face4j.response.RemoveTagResponse;
import com.github.mhendred.face4j.response.RemoveTagResponseImpl;
import com.github.mhendred.face4j.response.SaveTagResponse;
import com.github.mhendred.face4j.response.SaveTagResponseImpl;
import com.github.mhendred.face4j.response.StatusResponse;
import com.github.mhendred.face4j.response.StatusResponseImpl;
import com.github.mhendred.face4j.response.TrainResponse;
import com.github.mhendred.face4j.response.TrainResponseImpl;
import com.github.mhendred.face4j.response.UsersResponse;
import com.github.mhendred.face4j.response.UsersResponseImpl;

/**
 * Default implementation of {@link FaceClient} interface
 * 
 * @author Marlon Hendred 
 * 
 * @see {@link FaceClient}  
 * @see <a href="http://developers.face.com/docs/">Developer's page</a>
 */
public class DefaultFaceClient implements FaceClient, Serializable
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5381219949980766621L;

	/**
	 * Default API end point @TODO: set from properties
	 */
	private static final String API_ENDPOINT = "http://api.face.com";
	
	/**
	 * Handles {@code POST}s to the face.com endpoint
	 */
	private final Responder http;
	
	/**
	 * Facebook and twitter credentials
	 * 
	 * @TODO: Implement facebook OAuth dance
	 */
	private final Credentials creds;
	
	/**
	 * Parameters that are required for every call
	 */
	private final Parameters reqd;
	
	/**
	 * Base {@link URI} endpoint
	 */
	private final URI baseURI;

	/**
	 * Detector mode
	 */
	private boolean isAggressive;
	/**
	 * Convenience constructor with default {@link Responder} implementation
	 * 
	 * @see {@link DefaultResponder}
	 * @see {@link #DefaultFaceClient(String, String, Responder)}
	 */
	public DefaultFaceClient (final String apiKey, final String apiSecret)
	{
		this (apiKey, apiSecret, new ResponderImpl());
	}
	
	/**
	 * Constructs a Face.com API client pointing to {@code host}. You need to obtain an API key/secret pair.
	 * You can get an API key/secret from face.com 
	 * 
	 * @param apiKey Your aplication's API key
	 * @param apiSecret Your applications API secret
	 *  
	 * @see {@link Responder}
	 * @see <a href="http://developers.face.com/docs/">Developer's page</a> for information on obtaining an
	 * 		API key/secret
	 */
	public DefaultFaceClient (final String apiKey, final String apiSecret, final Responder responder)
	{
		Validate.notEmpty(apiKey, "API key cannot be empty");
		Validate.notEmpty(apiSecret, "API secret cannot be empty");
		
		this.baseURI = URI.create(API_ENDPOINT);
		this.http    = responder;
		this.creds   = new Credentials();
		this.reqd	 = new Parameters();
		
		reqd.put("api_key", apiKey);
		reqd.put("api_secret", apiSecret);
		
		setAggressive(false);
	}	
	
	/**
	 * @see {@link FaceClient#removeTags(String)}
	 */
	public List<RemovedTag> removeTags (final String tids) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(tids, "Tag ids cannot be empty");
		
		final Parameters params = new Parameters("tids", tids);
		final String json = executePost(FaceApi.REMOVE_TAGS, params);
		final RemoveTagResponse response = new RemoveTagResponseImpl(json);
		
		return response.getRemovedTags();	
	}
	
	/**
	 * @see {@link FaceClient#train(String)}
	 */
	public TrainResponse train (final String uids) throws FaceClientException, FaceServerException
	{
		final Parameters params = new Parameters("uids", uids);		
		final String json = executePost(FaceApi.TRAIN, params);
		final TrainResponse response = new TrainResponseImpl(json);
		
		return response;
	}
	
	/**
	 * @see {@link FaceClient#addTag(String, float, float, int, int, String, String, String)}
	 */
	public void addTag (
			final String url, 
			final float x, 
			final float y,
			final int width, 
			final int height, 
			final String uid, 
			final String label, 
			final String taggerId) 
		throws FaceClientException, FaceServerException
	{
		Validate.notNull(uid, "UID cannot be null");
		
		final Parameters params = new Parameters();
		
		params.put("x", x);
		params.put("y", y);
		params.put("width", width);
		params.put("height", height);
		params.put("tagger_id", taggerId);
		params.put("url", url);
		params.put("uid", uid);
		params.put("label", label);

		// No response
		executePost(FaceApi.ADD_TAG, params);
	}
	
	/**
	 * @see {@link FaceClient#getTags(String, String, String, String, boolean, int)}
	 */
	public List<Photo> getTags (
			final String urls,
			final String uids,
			final String order,
			final String filter,
			final boolean together,
			final int limit)
	throws FaceClientException, FaceServerException
	{
		return getTags(null, urls, uids, order, filter, together, limit);
	}
	
	/**
	 * @see {@link FaceClient#getTags(String, String, String, String, String, boolean, int)}
	 */
	public List<Photo> getTags (
			final String pids, 
			final String urls, 
			final String uids, 
			final String order,
			final String filter,
			final boolean together,
			final int limit) 
		throws FaceClientException, FaceServerException
	{
		final Parameters params = new Parameters();
		
		params.put("pids", pids);
		params.put("urls", urls);
		params.put("uids", uids);
		params.put("order", order);
		params.put("filter", filter);
		params.put("together", together);
		params.put("limit", limit);

		final String json = executePost(FaceApi.GET_TAGS, params);
		final GetTagsResponse response = new GetTagsResponseImpl(json);
		
		return response.getPhotos();
	}

	/**
	 * @see {@list FaceClient#saveTags(String, String, String)}
	 */
	public List<SavedTag> saveTags (final String tids, final String uid, final String label) 
		throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(uid, "User IDs cannot be null");
		Validate.notEmpty(tids, "Tag IDs cannot be null");
		
		final Parameters params = new Parameters("tids", tids);
		
		params.put("uid", uid);
		params.put("label", label);
		
		final String json = executePost(FaceApi.SAVE_TAGS, params);
		final SaveTagResponse response = new SaveTagResponseImpl(json);

		return response.getSavedTags();
	}
	
	/**
	 * @see {@link FaceClient#recognize(URL, String)}
	 */
	public Photo recognize (final File imageFile, final String uids) throws FaceClientException, FaceServerException
	{	
		Validate.notNull(imageFile, "File is null");
		Validate.isTrue(imageFile.exists(), "File does not exist!");
		Validate.notEmpty(uids, "User IDs cannot be null");
			
		final Parameters params = new Parameters("uids", uids);
		final String json =  executePost(imageFile, FaceApi.RECOGNIZE, params);
		final PhotoResponse response = new PhotoResponseImpl(json);		
		
		return response.getPhoto();
	}
	
	/**
	 * @see {@link FaceClient#recognize(String, String)}
	 */
	public List<Photo> recognize (final String urls, final String uids) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(urls, "URLs cant be empty");
		Validate.notEmpty(uids, "User IDs can't be empty");
				
		final Parameters params = new Parameters("uids", uids);
		
		params.put("urls", urls);

		final String json = executePost(FaceApi.RECOGNIZE, params);
		final PhotoResponse response = new PhotoResponseImpl(json);
				
		return response.getPhotos();
	}
	
	/**
h	 * @see {@link FaceClient#detect(URL)}
	 */
	public Photo detect (final File imageFile) throws FaceClientException, FaceServerException
	{	
		Validate.notNull(imageFile, "File is null");
		Validate.isTrue(imageFile.exists(), "File doesn't exist!");
		
		final String json = executePost(imageFile, FaceApi.DETECT, new Parameters());
		final PhotoResponse response = new PhotoResponseImpl(json);
		
		return response.getPhoto();
	}

	/**
	 * @see {@link FaceClient#detect(String)}
	 */
	public List<Photo> detect (final String urls) throws FaceClientException, FaceServerException
	{
		Validate.notNull(urls, "URLs cannot be null");
		
		final Parameters params = new Parameters();
		
		params.put("urls", urls);
		
		final String json = executePost(FaceApi.DETECT, params);
		final PhotoResponse response = new PhotoResponseImpl(json);
		
		return response.getPhotos();
	}

	/**
	 * @see {@link FaceClient#status(String)}
	 */
	public List<UserStatus> status (final String uids) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(uids, "UIDs cant be empty");
				
		final Parameters params = new Parameters();

		params.put("uids", uids);
		
		final String json = executePost(FaceApi.STATUS, params);
		final StatusResponse response = new StatusResponseImpl(json);
			
		return response.getTrainingStatus();
	}
	
	/**
	 * @see {@link FaceClient#facebookGet(String)}
	 */
	public List<Photo> facebookGet (final String uids) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(uids, "User IDs cannot be empty");
		
		final Parameters params = new Parameters();

		params.put("uids", uids);
		
		final String json = executePost(FaceApi.FACEBOOK, params);
		final PhotoResponse response = new PhotoResponseImpl(json);
						
		return response.getPhotos();	
	}

	/**
	 * @see {@link FaceClient#group(String, String)}
	 */
	public GroupResponse group(String urls, String uids) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(urls, "URLs cannot be empty");
		Validate.notEmpty(uids, "UIDs cannot be empty");
		
		final Parameters params = new Parameters();
		
		params.put("uids", uids);
		params.put("urls", urls);

		final String json = executePost(FaceApi.GROUP, params);
		final GroupResponse response = new GroupResponseImpl(json);
						
		return response;
	}

	/**
	 * @see {@link FaceClient#group(File, String)}
	 */
	public GroupResponse group (File imageFile, String uids) throws FaceClientException, FaceServerException 
	{
		Validate.isTrue(imageFile.exists(), "File does not exist");
		Validate.notEmpty(uids, "UIDs cannot be empty");
		
		final Parameters params = new Parameters();
		
		params.put("uids", uids);
		
		final String json = executePost(imageFile, FaceApi.GROUP, params);
		final GroupResponse response = new GroupResponseImpl(json);
			
		return response;
	}

	/** 
	 * @see {@link FaceClient#users(String)}
	 */
	public UsersResponse users (String namespaces) throws FaceClientException, FaceServerException
	{
		Validate.notEmpty(namespaces, "Must supply namespace(s)");
		
		final Parameters params = new Parameters();
		params.put("namespaces", namespaces);
		
		final String json = executePost(FaceApi.USERS, params);
		final UsersResponse response = new UsersResponseImpl(json, namespaces);
		
		return response;
	}
	
	/**
	 * @see {@link FaceClient#usage()}
	 */
	public LimitsResponse getLimits () throws FaceClientException, FaceServerException
	{
		final String json = executePost(FaceApi.LIMITS, new Parameters());
		final LimitsResponse response = new LimitsResponseImpl(json);
		
		return response;
	}
	
	/**
	 * @see {@link FaceClient#namespaces()}
	 */
	public List<Namespace> namespaces() throws FaceClientException, FaceServerException
	{
		final String json = executePost(FaceApi.NAMESPACES, new Parameters());
		final NamespaceResponse response = new NamespaceResponseImpl(json);
		
		return response.getNamespaces();
	}
	
	/**
	 * @see {@link FaceClient#getNamespace(String)
	 */
	public Namespace getNamespace(String namespace) throws FaceClientException, FaceServerException
	{
		for (Namespace ns : namespaces())
		{
			if (ns.getName().equals(namespace))
			{
				return ns;
			}
		}
		
		return null;
	}
	
	/**
	 * @see {@link FaceClient#setFacebookOauth2(String, String)}
	 */
	public void setFacebookOauth2(final String fbUserId, final String oauthToken)
	{
		creds.put("fb_user", fbUserId);
		creds.put("fb_oauth_token", oauthToken);
	}
	
	/**
	 * @see {@link FaceClient#setTwitterOauth(String, String, String)}
	 */
	public void setTwitterOauth(final String oauthUser, final String oauthSecret, final String oauthToken)
	{
		creds.put("twitter_oauth_user", oauthUser);
		creds.put("twitter_oauth_secret", oauthSecret);
		creds.put("twitter_oauth_token", oauthToken);
	}
	
	/**
	 * @see {@link FaceClient#clearFacebookCreds()}
	 */
	public void clearFacebookCreds()
	{
		creds.remove("fb_oauth_token");
		creds.remove("fb_user");
	}
	
	/**
	 * @see {@link FaceClient#clearTwitterCreds()}
	 */
	public void clearTwitterCreds()
	{
		creds.remove("twitter_oauth_user");
		creds.remove("twitter_oauth_secret");
		creds.remove("twitter_oauth_token");
	}
	
	/**
	 * @see {@link FaceClient#setAggressive(boolean)} 
	 */
	public void setAggressive(final boolean isAggressive)
	{
		this.isAggressive = isAggressive;
		
		reqd.put("detector", isAggressive ? "Aggressive" : "Normal");
	}
	
	/**
	 * @see {@link FaceClient#isAggressive()}
	 */
	public boolean isAggressive()
	{
		return isAggressive;	
	}
	
	private String executePost(FaceApi faceApi, Parameters params) throws FaceClientException, FaceServerException
	{
		return executePost(null, faceApi, params);
	}
	
	private String executePost(File file, FaceApi faceApi, Parameters params) throws FaceClientException, FaceServerException
	{
		final URI uri = baseURI.resolve(faceApi.getPath());
		
		params.putAll(reqd.getMap());

		if (faceApi.takesAuth() && !creds.isEmpty())
		{
			params.put("user_auth", creds.getAuthString());
		}
		
		if (file != null)
		{
			return http.doPost(file, uri, params.toPostParams());
		}
		
		else
		{
			return http.doPost(uri, params.toPostParams());
		}
	}
}