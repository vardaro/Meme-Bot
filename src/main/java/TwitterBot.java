/**
 * Created by Anthony on 2/17/2017
 *
 * Hello, this is a program I wrote using JRAW and Twitter4j that mirrors Hot posts from my favorite subreddits and tweets them on @freshpepperonis
 */

import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.*;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

public class TwitterBot {
  private static final String[] SUBREDDITS_TO_MONITOR = {"me_irl", "WholesomeMemes"};
  private static final int LIMIT_OF_SUBMISSIONS = 25;
  private static final String IMG_DIRECTORY = "\\TwitterBot\\src\\img";
  private static final String GRAVEYARD = "\\TwitterBot\\src\\graveyard.txt";
  public static void main(String[] args) throws TwitterException, OAuthException, InterruptedException {
    while(true) {
      // Run through both subreddits before taking a nap
      for (String aSUBREDDITS_TO_MONITOR : SUBREDDITS_TO_MONITOR) {
        Twitter twitter = connectTwitter();
        RedditClient redditClient = connectReddit();
        SubredditPaginator sp = new SubredditPaginator(redditClient, aSUBREDDITS_TO_MONITOR);
        sp.setLimit(LIMIT_OF_SUBMISSIONS);
        sp.setSorting(Sorting.HOT);
        sp.next(true);
        Listing<Submission> list = sp.getCurrentListing();
        for (int i = 0; i < LIMIT_OF_SUBMISSIONS; i++) {
          System.out.println("[bot] Finding a post...");
          if (postHasNotBeenPosted(list.get(i))) {
            System.out.println("[bot] ...Post found");
            String imgPath = getImage(list.get(i).getUrl());
            String tweet = list.get(i).getTitle();
            File image = new File(imgPath);

            // Tweet with media
            System.out.println("[bot] Tweeting: " + tweet + " with image " + imgPath);
            StatusUpdate status = new StatusUpdate(tweet);
            status.setMedia(image);
            twitter.updateStatus(status);

            // Wait one minute before tweeting again
            System.out.println("[bot] Sleeping for 30 seconds");
            Thread.sleep(30000);
          }
        }
      }
      System.out.println("[bot] Sleeping for 12 hours");
      Thread.sleep(1000 * 60 * 60 * 12);
    }
  }

  // Connects to Twitter API
  private static Twitter connectTwitter() {
    System.out.println("[bot] Connecting to Twitter...");
    Twitter twitter = new TwitterFactory().getSingleton();
    System.out.println("[bot] ...Successfully connected to Twitter");
    return twitter;
  }
  // Connects to Reddit API
  private static RedditClient connectReddit() throws OAuthException {
    System.out.println("[bot] Connecting to Reddit...");
    UserAgent myUserAgent = UserAgent.of("desktop", "bot", "v0.1", "username");
    RedditClient redditClient = new RedditClient(myUserAgent);
    Credentials credentials = Credentials.script("username", "password", "token", "secret");
    OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
    redditClient.authenticate(authData);
    System.out.println("[bot] ...Successfully connected to Reddit");
    return redditClient;
  }

  private static String getImage(String link) {
    // Check if the link has an image
    if(link.contains("i.reddituploads.com") || link.contains("imgur.com") || link.contains("i.redd.it")) {
      // Method downloads each image to the 'img' directory
      String filePath = null;
      String fileName;
      String extension;
      try {
        // Images from https://i.reddituploads.com/ are stupid and the '&'s get replaced replaced with '&amp;'
        // which is the HTML entity for the '&' symbol.
        link = link.replaceAll("&amp;","&");

        // Determine the file extension
        extension = link.contains(".png") ? ".png" : ".jpg";

        // This is so ugly, there is probably a better way to remove all the illegal chars from a string
        fileName = link.substring(link.lastIndexOf("/")).replaceAll(extension, "").replace('?','r');
        filePath = IMG_DIRECTORY + fileName + extension;
        System.out.println("[bot] Downloading " + extension + " image at " + link + " as " + filePath);

        // Download image to 'img' directory
        // Credit to http://www.technicalkeeda.com/java-tutorials/ for code that downloads image to directory
        URL url = new URL(link);
        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream(filePath);
        byte[] buffer = new byte[2048];
        int length;
        while((length = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, length);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return filePath;
    }
    // If we couldn't find and image return null
    System.out.println("[bot] No image found");
    return null;
  }

  // Every post on Reddit contains a unique post ID.
  // This method determines the post ID and checks if it has been posted before.
  // If it has not been posted before, write the ID to the .txt file to prevent duplicate tweets
  private static boolean postHasNotBeenPosted(Submission s) {
    File graveyard = new File(GRAVEYARD);
    PrintWriter out = null;
    // Unique ID written to txt file
    String subPostID = "r" + s.getId();

    // Check every line of the txt file to find a match
    try {
      Scanner scanner = new Scanner(graveyard);
      while(scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if(line.equals(subPostID)) {
          // Match found, skip this post
          System.out.println("[bot] This post (" + subPostID + ") has already been posted!");
          return false;
        }
      }
    }catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    // subPostID was not found in file, append to graveyard.txt
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(GRAVEYARD, true)));
      out.println(subPostID);
    }catch (IOException io) {
      io.printStackTrace();
    }
    // Clean up
    finally {
      if(out != null) {
        out.close();
      }
    }
    return true;
  }
}
