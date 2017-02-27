/**
 * Created by Anthony Vardaro on 2/27/2017
 * */

import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.*;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TwitterBot {
  public static void main(String[] args) throws TwitterException, OAuthException{
    Twitter twitter = connectTwitter();
    RedditClient redditClient = connectReddit();
    redditClient.me();
    //Status status = twitter.updateStatus("java test tweet using Apache Maven JRAW");
  }
  private static Twitter connectTwitter(){
    System.out.println("[bot] Connecting to Twitter..;");
    Twitter twitter = new TwitterFactory().getSingleton();
    System.out.println("[bot] ...Successfully connected to Twitter");
    return twitter;
  }
  private static RedditClient connectReddit()throws OAuthException {
    System.out.println("[bot] Connecting to Reddit...");
    UserAgent myUserAgent = UserAgent.of("desktop", "bot", "v0.1", "TheItalipino");
    RedditClient redditClient = new RedditClient(myUserAgent);
    Credentials credentials = Credentials.script("TheItalipino", "Reba321!", "skSYmiLn2tfQ5A","Fe8DtvU-A4p-N3SFMX3WouMjdkY");
    OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
    redditClient.authenticate(authData);
    System.out.println("[bot] ...Successfully connected to Reddit");
    return redditClient;
  }
}
