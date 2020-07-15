package com.google.ehub.servlets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.ehub.data.FavoriteItemDatastore;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
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
public class FavoriteItemServletTest {
  private static final String FAVORITE_ITEM_ID_PARAMETER_KEY = "favoriteItemId";
  private static final String ENTERTAINMENT_ITEM_KIND = "entertainmentItem";
  private static final String JSON_CONTENT_TYPE = "application/json";

  private static final String EMAIL = "Bryan@gmail.com";
  private static final String VALID_ITEM_ID_PARAMETER = "123";
  private static final String INVALID_ITEM_ID_PARAMETER = "wjnrwwoiofwij";

  private static final Long[] ITEM_IDS = {23L, 44L, 77L, 89L, 94L, 21301L};

  private final FavoriteItemServlet servlet = new FavoriteItemServlet();
  private final FavoriteItemDatastore favoriteItemDatastore = FavoriteItemDatastore.getInstance();
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())
          .setEnvEmail(EMAIL)
          .setEnvIsLoggedIn(true)
          .setEnvAuthDomain("gmail.com");

  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock PrintWriter printWriter;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void getRequestUserNotLoggedIn_ErrorIsSent() throws IOException {
    helper.setEnvIsLoggedIn(false);

    servlet.doGet(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void getRequestUserLoggedInWithNoFavoriteItems_ResponseSendsEmptyList()
      throws IOException {
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
    verify(printWriter).println("[]");
  }

  @Test
  public void getRequestUserLoggedInWithFavoriteItems_ResponseSendsListWithFavoriteItems()
      throws IOException {
    for (Long itemId : ITEM_IDS) {
      favoriteItemDatastore.addFavoriteItem(EMAIL, itemId);
    }

    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
    verify(printWriter).println(new Gson().toJson(favoriteItemDatastore.queryFavoriteIds(EMAIL)));
  }

  @Test
  public void postRequestWithNullParam_ErrorIsSent() throws IOException {
    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY)).thenReturn(null);

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithEmptyParam_ErrorIsSent() throws IOException {
    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY)).thenReturn("");

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithInvalidParam_ErrorIsSent() throws IOException {
    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY))
        .thenReturn(INVALID_ITEM_ID_PARAMETER);

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithValidParamButItemDoesNotExist_ErrorIsSent() throws IOException {
    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY)).thenReturn(VALID_ITEM_ID_PARAMETER);

    servlet.doPost(request, response);

    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void postRequestWithValidParamAndItemExists_FavoriteItemIsAdded() throws IOException {
    Entity itemEntity = new Entity(ENTERTAINMENT_ITEM_KIND);
    datastoreService.put(itemEntity);

    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY))
        .thenReturn(String.valueOf(itemEntity.getKey().getId()));

    servlet.doPost(request, response);

    Assert.assertEquals(1, FavoriteItemDatastore.getInstance().queryFavoriteIds(EMAIL).size());
  }

  @Test
  public void postRequestWithValidParamButUserNotLoggedIn_FavoriteItemIsNotAdded()
      throws IOException {
    Entity itemEntity = new Entity(ENTERTAINMENT_ITEM_KIND);
    datastoreService.put(itemEntity);

    helper.setEnvIsLoggedIn(false);

    when(request.getParameter(FAVORITE_ITEM_ID_PARAMETER_KEY))
        .thenReturn(String.valueOf(itemEntity.getKey().getId()));

    servlet.doPost(request, response);

    Assert.assertEquals(
        0, FavoriteItemDatastore.getInstance().queryEmails(itemEntity.getKey().getId()).size());
  }
}
