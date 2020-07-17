package com.google.ehub.servlets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import com.google.ehub.data.ProfileDatastore;
import com.google.ehub.data.UserProfile;
import com.google.ehub.servlets.LoginServlet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@RunWith(JUnit4.class)
public class ProfileServletTest {
  private static final String NAME_PROPERTY_KEY = "name";
  private static final String EMAIL_PROPERTY_KEY = "email";
  private static final String USERNAME_PROPERTY_KEY = "username";
  private static final String BIO_PROPERTY_KEY = "bio";
  private static final String EDIT_PROPERTY_KEY = "edit";
  private static final String FALSE_EDIT_VALUE = "false";
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String PROFILE_ITEM_KIND = "profile";
  private static final String NAME = "Honey";
  private static final String EMAIL = "honey7100@gmail.com";
  private static final String USERNAME = "honey7100";
  private static final String BIO = "My name means honey!";
  private static final String REDIRECT = "/ProfilePage.html";
  private static final String REDIRECT_GET = "/CreateProfilePage.html";
  private final ProfileServlet servlet = new ProfileServlet();
  private final ProfileDatastore profileData = new ProfileDatastore();

  private LocalServiceTestHelper helper = new LocalServiceTestHelper(
      new LocalDatastoreServiceTestConfig(), new LocalUserServiceTestConfig())
          .setEnvEmail(EMAIL)
          .setEnvIsLoggedIn(true)
          .setEnvAuthDomain("gmail.com");
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock PrintWriter printWriter;

  @Before
  public void init() throws IOException {
    MockitoAnnotations.initMocks(this);
    helper.setUp();
  }

  @After
  public void tearDown() throws IOException {
    helper.tearDown();
  }

  @Test
  public void postRequestWithNullParams_ErrorGetsSent() throws IOException {
    when(request.getParameter(NAME_PROPERTY_KEY)).thenReturn(null);
    when(request.getParameter(EMAIL_PROPERTY_KEY)).thenReturn(null);
    when(request.getParameter(BIO_PROPERTY_KEY)).thenReturn(null);
    when(request.getParameter(EDIT_PROPERTY_KEY)).thenReturn(null);
    when(request.getParameter(USERNAME_PROPERTY_KEY)).thenReturn(null);

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithEmptyParams_ErrorGetsSent() throws IOException {
    when(request.getParameter(NAME_PROPERTY_KEY)).thenReturn("");
    when(request.getParameter(EMAIL_PROPERTY_KEY)).thenReturn("");
    when(request.getParameter(BIO_PROPERTY_KEY)).thenReturn("");
    when(request.getParameter(EDIT_PROPERTY_KEY)).thenReturn("");
    when(request.getParameter(USERNAME_PROPERTY_KEY)).thenReturn("");

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithValidParams_SendsRedirect() throws IOException {
    Entity userEntity = new Entity(PROFILE_ITEM_KIND);

    userEntity.setProperty(NAME_PROPERTY_KEY, NAME);
    userEntity.setProperty(EMAIL_PROPERTY_KEY, EMAIL);
    userEntity.setProperty(USERNAME_PROPERTY_KEY, USERNAME);
    userEntity.setProperty(BIO_PROPERTY_KEY, BIO);

    datastoreService.put(userEntity);

    when(request.getParameter(NAME_PROPERTY_KEY)).thenReturn(NAME);
    when(request.getParameter(EMAIL_PROPERTY_KEY)).thenReturn(EMAIL);
    when(request.getParameter(BIO_PROPERTY_KEY)).thenReturn(BIO);
    when(request.getParameter(USERNAME_PROPERTY_KEY)).thenReturn(USERNAME);

    servlet.doPost(request, response);

    verify(response).sendRedirect(REDIRECT);
  }

  @Test
  public void postResquestWithFalseEditParam_SendsRedirect() throws IOException {
    Entity userEntity = new Entity(PROFILE_ITEM_KIND);

    userEntity.setProperty(NAME_PROPERTY_KEY, NAME);
    userEntity.setProperty(EMAIL_PROPERTY_KEY, EMAIL);
    userEntity.setProperty(USERNAME_PROPERTY_KEY, USERNAME);
    userEntity.setProperty(BIO_PROPERTY_KEY, BIO);

    datastoreService.put(userEntity);

    when(request.getParameter(NAME_PROPERTY_KEY)).thenReturn(NAME);
    when(request.getParameter(EMAIL_PROPERTY_KEY)).thenReturn(EMAIL);
    when(request.getParameter(BIO_PROPERTY_KEY)).thenReturn(BIO);
    when(request.getParameter(USERNAME_PROPERTY_KEY)).thenReturn(USERNAME);
    when(request.getParameter(EDIT_PROPERTY_KEY)).thenReturn(FALSE_EDIT_VALUE);

    servlet.doPost(request, response);

    verify(response).sendRedirect(REDIRECT);
  }

  @Test
  public void getRequestWithLoggedOutUser_ErrorGetsSent() throws IOException {
    helper.setEnvIsLoggedIn(false);

    servlet.doGet(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void getRequestWithLoggedInUser_SendsJsonResponse() throws IOException {
    helper.setEnvIsLoggedIn(true);

    Entity userEntity = new Entity(PROFILE_ITEM_KIND);
    userEntity.setProperty(NAME_PROPERTY_KEY, NAME);
    userEntity.setProperty(EMAIL_PROPERTY_KEY, EMAIL);
    userEntity.setProperty(USERNAME_PROPERTY_KEY, USERNAME);
    userEntity.setProperty(BIO_PROPERTY_KEY, BIO);

    datastoreService.put(userEntity);

    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
    verify(printWriter).println(new Gson().toJson(new UserProfile(NAME, USERNAME, BIO, EMAIL)));
  }

  @Test
  public void getRequestWithNullUser_ValidatesJson() throws IOException {
    helper.setEnvIsLoggedIn(true);

    JsonObject profileJson = new JsonObject();
    profileJson.addProperty("NeedsProfile", true);

    when(response.getWriter()).thenReturn(printWriter);
    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
  }
}