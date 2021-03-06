package lightning.plugins.cas.controllers;

import static lightning.server.Context.auth;
import static lightning.server.Context.redirect;
import static lightning.server.Context.redirectIfLoggedIn;
import static lightning.server.Context.redirectIfNotLoggedIn;
import static lightning.server.Context.request;
import static lightning.server.Context.response;
import static lightning.server.Context.url;
import static lightning.server.Context.users;
import lightning.ann.Controller;
import lightning.ann.Initializer;
import lightning.ann.Route;
import lightning.enums.HTTPMethod;
import lightning.plugins.cas.CASAuthenticator;
import lightning.plugins.cas.CASConfig;
import lightning.plugins.cas.CASUser;
import lightning.plugins.cas.ann.CASDomain;
import lightning.plugins.cas.ann.CASHost;
import lightning.plugins.cas.ann.CASPath;
import lightning.users.User;

import com.google.common.base.Optional;

// TODO: Should accept an optional redirect URL as a query parameter.
@Controller
public class CASAuthController {
  protected CASConfig casConfig;
  protected CASAuthenticator casAuth;
  protected String domain;
  protected final String PASSWORD = "12kasf8u1"; // Doesn't matter, because never used.
  
  @Initializer
  public void initialize(@CASHost String host, @CASPath String path, @CASDomain String domain) {
    casConfig = CASConfig.newBuilder()
        .setHost(host)
        .setPath(path)
        .build();
    
    this.domain = domain;
    
    casAuth = CASAuthenticator.newAuthenticator(casConfig);
  }
  
  @Route(path="/login", methods={HTTPMethod.GET})
  public void handleLogin() throws Exception {
    redirectIfLoggedIn(url().to("/"));
    
    // Attempt to log in.
    Optional<CASUser> casUserOpt = casAuth.startAuthentication(request(), response(), url().to("/"));
    
    if (casUserOpt.isPresent()) {
      // If we were successful in logging in, fetch the user data from CAS.
      CASUser casUser = casUserOpt.get();
      
      // Convert the CAS user to a native user.
      // Re-use the existing user record (if present), otherwise create a new user.
      // Note: We're just using a fixed constant password here, because the password will never be used
      // for anything since all authentication happens through CAS anyways but the native API requires it.
      Optional<User> userOpt = Optional.fromNullable(users().getByName(casUser.username));
      User user = userOpt.isPresent() ? 
          userOpt.get() : 
          users().create(casUser.username, casUser.username + domain, PASSWORD);
      
      // Log the user into the native user account that we created based on their CAS account.
      // Use attempt(...) to subject to security measures including throttling.
      auth().attempt(user.getUserName(), PASSWORD, true, null);
      
      // Redirect the user to their final destination.
      redirect(casUser.destinationUrl);
    }
  }
  
  @Route(path="/logout", methods={HTTPMethod.GET})
  public void handleLogout() throws Exception {
    redirectIfNotLoggedIn(url().to("/"));
    
    // Terminate the application session.
    auth().logout(true);
    
    // Terminate the CAS session.
    casAuth.endAuthentication(request(), response(), url().to("/"));
  }
}
