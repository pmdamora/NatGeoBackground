package pmdamora.natgeobackground;

import java.io.*;
import java.net.URL;
import java.util.Arrays;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * The NatGeoBackground program downloads the current National Geographic photo of the day
 * and sets it as the desktop background.
 *
 * @author  Paul D'Amora
 * @version 1.0
 * @since   2017-09-21
 */
public class Main {


    /**
     * The constructor method calls all other methods in the program
     *
     * @param args Unused
     * @throws Exception on connection error
     */
    public static void main(String[] args) throws Exception {
        // Define variables
        String baseUrl = "http://www.nationalgeographic.com/photography/photo-of-the-day/_jcr_content/.gallery.";
        int[] sizeOptions = {800, 1024, 1600, 2048};


        // Run the program
        String jsonUrl = buildJSONUrl(baseUrl);
        int displayWidth = chooseImageSize(sizeOptions);
        String strImageUrl = getUrl(jsonUrl, displayWidth);
        String path = downloadImage(strImageUrl);
        changeDesktopBackground(path);

        System.out.println("[Process Completed]");
    }


    /**
     * This method builds the full JSON url by adding the formatted date to the base url
     *
     * @param baseUrl The base of the url without the date
     * @return The full url of the JSON file
     */
    private static String buildJSONUrl(String baseUrl) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDateTime now = LocalDateTime.now();

        return baseUrl + dtf.format(now) + ".json";
    }


    /**
     * This method chooses an appropriate image size from a list of options
     * based on which is closest to the user's display resolution.
     *
     * @param sizeOptions The available options for image size
     * @return int This returns the chosen image width
     */
    private static int chooseImageSize(int[] sizeOptions) {
        System.out.println("Determining desktop resolution...");
        // Get the width the user's primary display
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();

        // Find the closest size, using larger sizes if possible
        Arrays.sort(sizeOptions);
        int distance = Math.abs(sizeOptions[0] - width);
        int i = 0;
        boolean isLargerThan = (distance > width);

        for (int x = 1; x < sizeOptions.length; x++) {

            // We want to prioritize larger images
            if ((sizeOptions[x] >= width) && (!isLargerThan)) {
                isLargerThan = true;
                distance = Math.abs(sizeOptions[x] - width);
                i = x;
            } else {
                int x_distance = Math.abs(sizeOptions[x] - width);
                if (x_distance < distance) {
                    i = x;
                    distance = x_distance;
                }
            }
        }

        return sizeOptions[i];
    }

    /**
     * This methods downloads and parses the provided JSON file, and locates the
     * url of the appropriate image.
     *
     * @param jsonUrl The base url string
     * @param displayWidth The width of user's display
     * @return String This returns the url as a string.
     * @throws Exception on connection error
     */
    private static String getUrl(String jsonUrl, int displayWidth) throws Exception {
        String strImageUrl = "";
        BufferedReader reader = null;

        // Fetch image url
        System.out.println("Fetching image url...");

        try {
            // Download the JSON file as a String
            URL url = new URL(jsonUrl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();

            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            String json = buffer.toString();

            // Parse the JSON and convert to a Java object
            Gson gson = new Gson();
            Gallery page = gson.fromJson(json, Gallery.class);

            // Choose the appropriate image size, and build the url
            String baseUrl = page.items.get(0).url;
            String sizeUrl = page.items.get(0).sizes.get(Integer.toString(displayWidth));
            strImageUrl = baseUrl.concat(sizeUrl);

            System.out.println("Successfully fetched image url.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Clean up even if an exception occurs
            if (reader != null) { // BufferedReader
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return strImageUrl;
    }


    /**
     * This method takes the url of the image and downloads it to the user's
     * filesystem in a specified location.
     *
     * @param strImageUrl The url location of the image
     * @return String This returns the file path of the downloaded image
     */
    private static String downloadImage(String strImageUrl) {
        String filePath = "";
        InputStream in = null;
        ByteArrayOutputStream out = null;
        FileOutputStream fos = null;

        // Download image
        System.out.println("Downloading image from url...");

        // Ensure that the url exists
        if (!strImageUrl.equals("")) {
            try {
                // Download the image and output to buffer
                URL urlImage = new URL(strImageUrl);
                in = new BufferedInputStream(urlImage.openStream());
                out = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int n;

                while (-1 != (n = in.read(buffer)))
                    out.write(buffer, 0, n);

                byte[] response = out.toByteArray();

                System.out.println("Successfully downloaded image.");

                // Determine file path
                filePath = createFilePath();
                System.out.println("Saving image to: '" + filePath + "'...");

                // Download byte array to filesystem
                fos = new FileOutputStream(filePath);
                fos.write(response);

                System.out.println("Successfully saved image.");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Clean up even if an exception occurs
                if (fos != null) { // FileOutputStream
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (in != null) { // InputStream
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (out != null) { // ByteArrayOutputStream
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("Error: unable to download image from invalid url.");
            System.exit(0);
        }

        return filePath;
    }


    /**
     * This method changes the user's desktop background to the image located at
     * the specified path
     *
     * @param path The path of the image to be used
     */
    private static void changeDesktopBackground(String path) {
        // Change the background
        System.out.println("Changing desktop background...");

        // Ensure we have a valid path
        if (!path.equals("")) {
            try {
                File file = new File(path); // open the image as a File

                // TODO Determine user's OS and act accordingly
                String as[] = {
                        "osascript",
                        "-e", "tell application \"Finder\"",
                        "-e", "set desktop picture to POSIX file \"" + file.getAbsolutePath() + "\"",
                        "-e", "end tell"
                };

                // Execute the above script
                Runtime runtime = Runtime.getRuntime();
                runtime.exec(as);

                System.out.println("Successfully change desktop background.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: unable to retrieve image at invalid path.");
            System.exit(0);
        }
    }


    /**
     * This method determines the exact file path of the downloaded image
     * primarily based on the current date.
     *
     * @return String This returns the full filepath of the new image
     */
    private static String createFilePath() {
        String homeDir = System.getProperty("user.home");

        // TODO: Path will be different on other OS
        String backgroundDir = homeDir.concat("/Pictures/National Geographic Backgrounds/");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        LocalDateTime now = LocalDateTime.now();

        String year = Integer.toString(now.getYear());
        String month = Integer.toString(now.getMonthValue());

        backgroundDir = backgroundDir + year + "/" + month;
        String fileName = dtf.format(now) + ".jpg";

        // Check if the directory exists, and create if necessary
        File directory = new File(backgroundDir);
        if (!directory.exists()){
            boolean success = directory.mkdirs();
            if (!success) {
                System.out.println("Error: Directory creation failed.");
                System.exit(0);
            }
        }
        return backgroundDir + "/" + fileName;
    }
}
