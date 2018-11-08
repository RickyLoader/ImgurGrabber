import java.io.*;
import java.util.Scanner;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URL;

/**
 * ImgurGrabber.java - Pulls all individual image URLs from an imgur.com album, can pull ~1000 links in <5 seconds,
 * also appends image extension to existing imgur URLs in a file for upload compatibility with lensdump.
 *
 * @author Ricky Loader
 * @version 1.0
 */
public class ImgurGrabber {

    /**
     * Prompts user for input of an imgur album URL and returns the direct image URLs of each image.
     *
     * @param args Command line input.
     */
    public static void main(String[] args) {

        /* Take an imgur album URL or file name as input. */
        Scanner input = new Scanner(System.in);
        System.out.println("\n1. Grab links from an IMGUR album:\n\n" +
                "2. Append image file types to URLS in a text file:");
        String desired = input.nextLine();

        /* Wait for a valid option. */
        while(!desired.equals("1") && !desired.equals("2")){
            System.out.println("Please select a valid option:\n");
            desired=input.nextLine();
        }

        if(desired.equals("1")) {
            System.out.println("Please enter an IMGUR album link to obtain image URLs from:\n");
            desired = input.nextLine().replace(" ", "");

            /* Verify the URL is valid. */
            while (!isLink(desired, true)) {
                System.out.println("Invalid IMGUR url please try again:\n");
                desired = input.nextLine().replace(" ", "");
            }
            System.out.println("\nFetching links...\n");
            long startTime = System.currentTimeMillis();

            /* Pull the HTML from the URL. */
            String html = visitLink(desired);

            /* Find and create each image's URL from the HTML. */
            int links = findLinks(html);

            long endTime = System.currentTimeMillis();
            System.out.println("\nFound "+ links + " images in " + (endTime-startTime) +"ms.");
        }
        else{
            System.out.println("Please enter a file name which contains imgur URLs:\n");
            desired=input.nextLine();

            /* Verify the file exists and is accessible. */
            while(!isFile(desired)){
                System.out.println("Invalid file name please try again:\n");
                desired = input.nextLine();
            }
            System.out.println("\nAmending links...\n");
            int urls = processFile(desired);
            System.out.println("\n"+urls + " URLS have been amended in " + desired+" and saved to amended"+desired+"!" +"\n");
        }

        input.close();
    }

    /**
     * Verify that a given imgur URL matches the correct format for either an album or an individual image.
     *
     * @param url   A String URL to be verified.
     * @param album A Boolean specifying whether the URL is for an album or not.
     * @return A Boolean specifying whether the URL is correct or not.
     */
    private static boolean isLink(String url, boolean album) {
        boolean result = false;
        /* REGEX for an imgur single image URL. */
        String linkFormat = "https://imgur.com/[a-zA-Z0-9]+.png";

        /* REGEX for an imgur album URL. */
        String albumFormat = "https://imgur.com/a/\\S+";

        /* Use a map to find appropriate REGEX for given Boolean. */
        HashMap<Boolean, String> formats = new HashMap<>();
        formats.put(true, albumFormat);
        formats.put(false, linkFormat);

        if (url.matches(formats.get(album))) {
            result = true;
        }
        return result;
    }

    /**
     * Accesses a given text file of image URLs and creates a new text file with the URLs now amended to be
     * compatible with lensdump uploading.
     *
     * @param fileName The file to be accessed.
     * @return The number of URLs found and amended.
     */
    private static int processFile(String fileName){
        try {
            ArrayList<String> amended = new ArrayList<>();
            File file = new File(fileName);
            Scanner scan = new Scanner(file);
            while(scan.hasNextLine()){
                String url = scan.nextLine();
                url+=".png";
                amended.add(url);
            }
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(
                            new File("amended"+fileName)));
            for(String s: amended){
                bw.write(s);
                bw.newLine();
            }
            bw.close();
            return amended.size();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Verifies that a given file name belongs to an existing file and is accessible.
     * @param toCheck File name to be verified.
     * @return A Boolean specifying the validity of the file.
     */
    private static boolean isFile(String toCheck){
        boolean result = false;
        File file = new File(toCheck);
        if(file.exists() && file.canRead() && file.canWrite() && file.canExecute()){
            result=true;
        }
        return result;
    }

    /**
     * Visits the given URL and returns a String containing the HTML content found.
     *
     * @param desired The String URL to be visited.
     * @return A String containing the HTML content found.
     */
    private static String visitLink(String desired) {
        StringBuilder sb = new StringBuilder();

        try {
            URL url = new URL(desired);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
            }
            bufferedReader.close();
            return sb.toString();
        } catch (IOException e) {
            System.out.println("Unable to open URL");
        }
        return sb.toString();
    }

    /**
     * Searches a String containing HTML for each individual image URL. imgur only stores the images
     * currently visible on screen in <img src= > tags. The images are stored as objects with attributes
     * such as title, description, hash, etc. The hash attribute's value can be appended to an imgur prefix
     * to obtain a direct URL to the image.
     *
     * @param html A String containing the HTML to be searched.
     * @return The number of images found in the given HTML.
     */
    private static int findLinks(String html) {

        /* The album's images are stored in two blocks within the HTML, each beginning with this prefix. */
        String albumRegex = "\"count\":[0-9]+,\"images\":\\[";

        /*
         * Splitting on albumRegex produces 3 Strings, the HTML prior to the first occurrence, and two duplicate
         * blocks of HTML containing the image objects of the album (only the first is needed).
         */
        String album = html.split(albumRegex)[1];

        /* Image objects are comma separated. */
        String[] imageObjects = album.split("},");

        ArrayList<String> unique = new ArrayList<>();

        /* Each image object begins with this String followed by the hash value and the remaining attributes. */
        String imageRegex = "{\"hash\":\"";

        /* The prefix for which to append a hash value to gain the image's URL. */
        String imgurPrefix = "https://imgur.com/";

        /* Aids in compatibility with other services. */
        String imageSuffix = ".png";

        /* For each image object. */
        for (String object : imageObjects) {

            /*
             * Create the URL by removing the imageRegex which leaves the hash value and the remaining attributes.
             * Split on quote marks the end of the hash value. This leaves the hash value
             * in the 0 position which can then be appended to the imgurPrefix to gain the image's URL.
             */
            String url = imgurPrefix + (object.replace(imageRegex, "").split("\"")[0]+imageSuffix);

            /* Verify that the URL is not a duplicate and is a valid imgur link. */
            if (!unique.contains(url) && isLink(url, false)) {
                unique.add(url);
                System.out.println(url);
            }
        }
        return unique.size();
    }
}
