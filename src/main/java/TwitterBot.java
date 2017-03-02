/**
 * Created by Anthony on 2/17/2017
 *
 * Hello, this is a program I wrote using JRAW and Twitter4j that mirrors Hot posts from my favorite subreddits and tweets them on its
 * bot account @freshpepperonis
 */

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
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

  // Counts printlns
  private static int line = 1;

  // List subreddits you want to check
  private static final String[] SUBREDDITS_TO_MONITOR = {"CringeAnarchy", "justneckbeardthings", "WholesomeMemes", "me_irl"};

  // How many posts to search for
  private static final int LIMIT_OF_SUBMISSIONS = 1;

  // Path for your folder where images are downloaded
  private static final String IMG_DIRECTORY = "C:\\Users\\justa\\IdeaProjects\\TwitterBot\\src\\img";

  // Path for your txt file where post history is written to
  private static final String GRAVEYARD = "C:\\Users\\justa\\IdeaProjects\\TwitterBot\\src\\graveyard.txt";

  // Place info for Reddit authentication
  private static final String USERNAME = "";
  private static final String PASSWORD = "";
  private static final String AGENT_ID = "";
  private static final String TOKEN_SECRET = "";

  public static void main(String[] args) throws TwitterException, OAuthException, InterruptedException, IOException {
    while(true) {
      // Connect to Twitter and Reddit
      Twitter twitter = connectTwitter();
      RedditClient redditClient = connectReddit();

      // Run through both subreddits before taking a nap
      for (String aSUBREDDITS_TO_MONITOR : SUBREDDITS_TO_MONITOR) {

       int submissionCount = LIMIT_OF_SUBMISSIONS;

        // Check each post and determine if it can be tweeted
        for (int i = 0; i < submissionCount; i++) {
          // Get Listing of Hot posts from Subreddit
          System.out.println(line++ +" [bot] Checking sub: r/" + aSUBREDDITS_TO_MONITOR);
          SubredditPaginator sp = new SubredditPaginator(redditClient, aSUBREDDITS_TO_MONITOR);
          sp.setLimit(submissionCount);
          sp.setSorting(Sorting.HOT);
          sp.next(true);
          Listing<Submission> list = sp.getCurrentListing();
          System.out.println(line++);
          System.out.println(line++ +" [bot] Tweet Attempt #" + (i+1) );
          if (postHasNotBeenPosted(list.get(i)) && isUnder140(list.get(i))) {
            // Download the image
            System.out.println(line++ +" [bot] Post found");
            String imgPath = getImage(list.get(i).getUrl());

            // Check if post contains image
            if(imgPath != null) {
              String tweet = list.get(i).getTitle();
              File image = new File(imgPath);

              // Tweet with media
              System.out.println(line++);
              System.out.println(line++ +" [bot] Tweeting from r/" +aSUBREDDITS_TO_MONITOR +": " + tweet + " with image " + imgPath);
              System.out.println(line++);
              StatusUpdate status = new StatusUpdate(tweet);
              status.setMedia(image);
              twitter.updateStatus(status);
            } else {
              // Lengthen the for loop until we find a match
              System.out.println(line++);
              System.out.println(line++ +" [bot] Post doesn't contain an image. Looking for another post.");
              System.out.println(line++);
              sp.setLimit(submissionCount++);
            }
          } else {
            // Lengthen the For loop until we find a match
            System.out.println(line++ +" [bot] Post was found but has already been tweeted. Looking for another post.");
            sp.setLimit(submissionCount++);
          }
        }
      }
      // Take ten minutes rest before pinging Reddit again
      System.out.println(line++ +" [bot] Sleeping for 10 minutes");
      Thread.sleep(1000 * 60 * 10);
    }
  }

  // Connects to Twitter API
  private static Twitter connectTwitter() {
    System.out.println(line++ +" [bot] Connecting to Twitter");
    Twitter twitter = new TwitterFactory().getSingleton();
    System.out.println(line++ +" [bot] Successfully connected to Twitter");
    return twitter;
  }
  // Connects to Reddit API
  private static RedditClient connectReddit() throws OAuthException, IOException {
    System.out.println(line++ +" [bot] Connecting to Reddit");
    UserAgent myUserAgent = UserAgent.of("desktop", "bot", "v0.1", "TheItalipino");
    RedditClient redditClient = new RedditClient(myUserAgent);
    Credentials credentials = Credentials.script(USERNAME, PASSWORD, AGENT_ID, TOKEN_SECRET);
    OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
    redditClient.authenticate(authData);
    System.out.println(line++ +" [bot] Successfully connected to Reddit");
    return redditClient;
  }

  private static String getImage(String link) {
    // Check if the link has an image
    if(link.contains("i.reddituploads.com") || link.contains("i.imgur.com") || link.contains("i.redd.it")) {
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
        System.out.println(line++ +" [bot] Downloading " + extension + " image at " + link + " as " + filePath);

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
    System.out.println(line++ +" [bot] No image found");
    return null;
  }

  // Every post on Reddit contains a unique alphanumeric post ID
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
        String inline = scanner.nextLine();
        if(inline.equals(subPostID)) {
          // Match found, skip this post
          System.out.println(line++ +" [bot] This post (" + subPostID + ") has already been posted!");
          return false;
        }
      }
    }catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    // subPostID was not found in file, append to graveyard.txt to prevent future duplicate tweets
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
  // Determines whether Submission title is less than 140 characters
  private static boolean isUnder140(Submission s) {
    return s.getTitle().length() <= 140;
  }
}
