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
import java.util.Properties;
import java.util.Scanner;

public class TwitterBot {
  // List subreddits you want to check. Put as many as you like :)
  private static final String[] SUBREDDITS_TO_MONITOR = {"me_irl", "WholesomeMemes", "me_irl", "fakehistoryporn"};

  // How many posts to search for
  private static final int LIMIT_OF_SUBMISSIONS = 1;

  // Put the name of you properties file here.
  private static final GetProperties prop = new GetProperties("BotInfo.properties");

  // Path for your folder where images are downloaded
  private static final String IMG_DIRECTORY = prop.get("IMG_DIRECTORY");

  // Path for your txt file where post history is written to
  private static final String MemeCemetary = prop.get("MemeCemetary");

  // Place info for Reddit authentication
  private static final String USERNAME = prop.get("USERNAME");
  private static final String PASSWORD = prop.get("PASSWORD");
  private static final String AGENT_ID = prop.get("AGENT_ID");
  private static final String TOKEN_SECRET = prop.get("TOKEN_SECRET");

  // Counts printlns
  private static int line = 1;

  public static void main(String[] args) throws InterruptedException, IOException {
      // Connect to Twitter and Reddit
      Twitter twitter = connectTwitter();
      RedditClient redditClient = connectReddit();

      // Run through both subreddits before taking a nap
      for (String aSUBREDDITS_TO_MONITOR : SUBREDDITS_TO_MONITOR) {

        int submissionCount = LIMIT_OF_SUBMISSIONS;
        botSay("----------------------------------------------------------------Checking sub: r/" + aSUBREDDITS_TO_MONITOR + "----------------------------------------------------------------");

        // Check each post and determine if it can be tweeted
        for (int i = 0; i < submissionCount; i++) {

          // Get Listing of Hot posts from Subreddit
          SubredditPaginator sp = new SubredditPaginator(redditClient, aSUBREDDITS_TO_MONITOR);
          int tweetAttempt = i + 1;
          sp.setLimit(submissionCount);
          sp.setSorting(Sorting.HOT);
          sp.next(true);
          Listing<Submission> list = sp.getCurrentListing();
          botSay("");
          botSay("r/" + aSUBREDDITS_TO_MONITOR + " Tweet Attempt #" + (tweetAttempt));
          Submission s = list.get(i);

          // 100 is the limit for post listing you can get from Reddit API
          if (tweetAttempt != 100) {
            if (postHasNotBeenPosted(s) && isUnder140(s)) {

              // Download the image
              botSay("Post found");
              String imgPath = getImage(s.getUrl());

              // Check if post contains image
              if (imgPath != null) {
                String tweet = s.getTitle();
                File image = new File(imgPath);
                // Tweet with media
                botSay("");
                botSay("Tweeting from r/" + aSUBREDDITS_TO_MONITOR + ": " + tweet + " with image " + imgPath);
                botSay("");
                StatusUpdate status = new StatusUpdate(tweet);
                status.setMedia(image);
                try {
                  // Tweet
                  twitter.updateStatus(status);

                  // Save record to file
                  record(s);

                }
                // Most commonly a TwitterException is thrown due to update limits
                // The program waits fifteen minutes (Twitter Rate Limit timeframe)
                // and reruns. Since the post was never recorded it'll find the same
                // tweet and try again.
                catch (TwitterException te) {
                  if (te.exceededRateLimitation()) {
                    // Waits fifteen minutes to check for rate limits and tries again
                    botSay("Exceeded Twitter API Rate Limitation! Resting for 15 minutes");
                    Thread.sleep(60000 * 15);
                    botSay("Trying post again");
                  }
                }
              } else {
                // Lengthen the for loop until we find a match
                botSay("");
                botSay("Post doesn't contain an image. Looking for another post.");
                botSay("");
                sp.setLimit(submissionCount++);
              }
            } else {
              // Lengthen the For loop until we find a match
              botSay("Post was found but has already been tweeted. Looking for another post.");
              sp.setLimit(submissionCount++);
            }
          } else {
            botSay("");
            botSay("Reached max post listing of 100 posts.");
            botSay("Skipping subreddit: r/" + aSUBREDDITS_TO_MONITOR);
          }
        }
      }
      // Take ten minutes rest before pinging Reddit again
      botSay("Sleeping for 6 hours");
      Thread.sleep(60000 * 60 * 6);
      // Run again
      Thread.currentThread().join();
    }


  // Connects to Twitter API
  private static Twitter connectTwitter() {
    botSay("Connecting to Twitter");
    Twitter twitter = new TwitterFactory().getSingleton();
    botSay("Successfully connected to Twitter");
    return twitter;
  }

  // Connects to Reddit API
  private static RedditClient connectReddit() {
    try {
      botSay("Connecting to Reddit");
      UserAgent myUserAgent = UserAgent.of("desktop", "bot", "v0.1", "TheItalipino");
      RedditClient redditClient = new RedditClient(myUserAgent);
      Credentials credentials = Credentials.script(USERNAME, PASSWORD, AGENT_ID, TOKEN_SECRET);
      OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
      redditClient.authenticate(authData);
      botSay("Successfully connected to Reddit");
      botSay("*hacker voice* im in ");
      return redditClient;

    } catch (OAuthException oae) {
      // If the user puts wrong credentials
      botSay("Invalid Reddit API credentials :(");
      return null;

    } catch (RuntimeException o) {
      // If there is no connecting it'll keep trying until it can connect
      botSay("Error connecting to Reddit");
      botSay("Trying again...");
      return connectReddit();
    }
  }

  private static String getImage(String link) {
    // Check if the link has an image
    if (link.contains("i.reddituploads.com") || link.contains("i.imgur.com") || link.contains("i.redd.it") && !link.contains("gif")) {
      // Method downloads each image to the 'img' directory
      String filePath = null;
      String fileName;
      String extension;
      try {
        // Images from https://i.reddituploads.com/ are stupid and the '&'s get replaced replaced with '&amp;'
        // which is the HTML entity for the '&' symbol.
        link = link.replaceAll("&amp;", "&");

        // Determine the file extension
        extension = link.contains(".png") ? ".png" : ".jpg";

        // This is so ugly, there is probably a better way to remove all the illegal chars from a string
        fileName = link.substring(link.lastIndexOf("/")).replaceAll(extension, "").replace('?', 'r');
        filePath = IMG_DIRECTORY + fileName + extension;
        botSay("Downloading " + extension + " image at " + link + " as " + filePath);

        // Download image to 'img' directory
        URL url = new URL(link);
        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream(filePath);
        byte[] buffer = new byte[2048];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, length);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return filePath;
    }
    // If we couldn't find and image return null
    botSay("[bot] No image found smh");
    return null;
  }

  // Every post on Reddit contains a unique alphanumeric post ID
  // This method determines the post ID and checks if it has been posted before.
  // If it has not been posted before, write the ID to the .txt file to prevent duplicate tweets
  private static boolean postHasNotBeenPosted(Submission s) throws IOException {
    File graveyard = new File(MemeCemetary);
    // Unique ID written to txt file

    String subPostID = getSubPostID(s);

    // Check every line of the txt file to find a match
    try {
      Scanner scanner = new Scanner(graveyard);
      while (scanner.hasNextLine()) {
        String inline = scanner.nextLine();
        if (inline.equals(subPostID)) {
          // Match found, skip this post
          botSay("This post (" + subPostID + ") has already been posted!");
          return false;
        }
      }
      // If the file is not found we create the file
    } catch (FileNotFoundException e) {
      botSay(MemeCemetary + " file is not found!");
      botSay("Creating file...");

      // File creation
      File file = new File(MemeCemetary);
      if (file.createNewFile()) {
        botSay("File is successfully created!");
        botSay("Retrying post ID" + subPostID);

        // Should always return false since its a new file with nothing written
        return postHasNotBeenPosted(s);
      }
    }
    return true;
  }

  // Writes the post ID to the graveyard
  // This method only get called after a post has been tweeted
  private static void record(Submission s) throws IOException {
    String subPostID = getSubPostID(s);

    // Write subPostID
    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(MemeCemetary, true)));
    out.println(subPostID);

    // Clean up when done
    out.close();
  }

  // Determines whether Submission title is less than 140 characters
  private static boolean isUnder140(Submission s) {
    return s.getTitle().length() <= 140;
  }

  // Returns unique post ID written to file
  private static String getSubPostID(Submission s) {
    return "r" + s.getId();
  }

  // Prints to the console with line count and " [bot] "
  private static void botSay(String sout) {
    System.out.println(line++ + " [bot] " + sout);
  }
}

// This class reads in the specified properties file
class GetProperties {
  Properties properties = new Properties();
  // Path for properties file
  private String propPath;

  // Assign specified properties file name to "propPath"
  public GetProperties(String propertiesPath) {
    this.propPath = propertiesPath;
  }

  // Returns requested property from file
  public String get(String val) {
    try {
      InputStream input = getClass().getClassLoader().getResourceAsStream(propPath);
      properties.load(input);
      return properties.getProperty(val);
    } catch (IOException io) {
      System.out.println("Error getting property\nException: " + io);
      return null;
    }
  }
}
