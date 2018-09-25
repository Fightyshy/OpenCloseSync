import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Launcher {

    private static final String APPLICATION_NAME = "OpenCloseSync/1.1";
    private static HttpTransport httpTransport;
    private static final String DATA_STORE_DIR = "./userData"; //Storage for user creds, users home directory in subdirect
    private static FileDataStoreFactory dataStoreFactory; //Best practice, shared across app
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_METADATA, DriveScopes.DRIVE);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance(); //wtf is this???
    static Scanner userInput = new Scanner(System.in);

    private static Credential authorize() throws Exception{ //authorizes app to access user data
        InputStream sekritLoc = Launcher.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets loadSekrits = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(sekritLoc));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, loadSekrits, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static void main(String args[]) {
        System.out.println("1 for download file, 2 for upload file, 3 for exit.");
        System.out.print("Select an option: ");
        int option = userInput.nextInt();

        try{
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            dataStoreFactory = new FileDataStoreFactory(new File(DATA_STORE_DIR));

            Credential credential = authorize();

            Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
            DriveManager driver = new DriveManager();
            // End auth, start activity
            switch (option){
                case 1: {
                    String loc = driver.downloadFile(service);
                    driver.runProcessor(loc);
                    break;
                }
                case 2: {
                    driver.killProcessor();
                    driver.uploadFile(service);
                    break;
                }
                case 3: System.exit(0);
            }
        } catch(IOException e){
            System.out.println(e.getMessage());
            System.out.println("An error has occurred.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
