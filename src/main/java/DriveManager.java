import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.awt.*;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.Collections;

//text/plain, not markdown
class DriveManager {

    private final String storagePath = System.getProperty("user.dir")+java.io.File.separator+"diskStorage";
    private final String userPath = System.getProperty("user.dir")+java.io.File.separator+"userData";

    //@TODO downloadFile and uploadFile -> find platform neutral clear console or update to different console logging

    String downloadFile(Drive service) throws IOException {
        //actual hard modo - on startup, go through every file that exists and download a fresh copy
        //@TODO Add warning feature for overwriting existing file
        File foundFile;
        String folderName;

        System.out.println("Folders in GDrive below: ");
        driveListSearcher(false,"",service);
        System.out.println();

        System.out.print("Enter folder name to display contents: ");
        folderName = Launcher.userInput.next();
        folderName = driveSingleSearcher(false, "", folderName, service).getId(); //current workaround
        driveListSearcher(true,folderName,service); //@TODO only takes ID, not name, find reason why and fix

        System.out.println();
        System.out.print("Enter file name: ");
        Launcher.userInput.nextLine();
        String searchName = Launcher.userInput.nextLine();
        Launcher.userInput.close();

        foundFile = driveSingleSearcher(true,folderName,searchName,service);

        java.io.File dir = new java.io.File(storagePath);
        dir.mkdir();//doesn't replace if exists
        OutputStream outputStream = new FileOutputStream(new java.io.File(storagePath+java.io.File.separator+foundFile.getName()));
        service.files().get(foundFile.getId()).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return foundFile.getName();
    }

    void uploadFile(Drive service){
        java.io.File fileToUpload;
        File foundFolder;
        File fileCrosscheck;
        try {
            FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
            dialog.setMode(FileDialog.LOAD);
            dialog.setDirectory(storagePath);
            dialog.setVisible(true);
            java.io.File[] file = dialog.getFiles();
            fileToUpload = new java.io.File(storagePath+java.io.File.separator+file[0].getName());

            System.out.println("Folders found below:");
            driveListSearcher(false,"", service);
            System.out.println();
            System.out.print("Enter folder name to store in (blank for root): ");
            String folderName = Launcher.userInput.next();
            foundFolder = driveSingleSearcher(false, "", folderName, service);
            System.out.println();

            fileCrosscheck = driveSingleSearcher(true, foundFolder.getId(), fileToUpload!=null?fileToUpload.getName():"", service);
            System.out.println("Upload confirmed successful");

            if (fileCrosscheck.getName() != null) {
                FileContent toUpdate = new FileContent("text/plain", fileToUpload);
                File content = new File();
                content.setName(fileToUpload.getName());
                content.setMimeType("text/plain");
                service.files().update(fileCrosscheck.getId(), content, toUpdate).execute();
                System.out.println("Overwritten old file and deleting physical...");
            } else {
                File metadata = new File();
                metadata.setName(fileToUpload!=null?fileToUpload.getName():"");
                metadata.setMimeType("text/plain");
                metadata.setParents(Collections.singletonList(foundFolder.getId()));
                FileContent content = new FileContent("text/plain", fileToUpload);
                service.files().create(metadata, content).setFields("id, parents").execute();
                System.out.println("No old file was found so a new one was created, deleting physical...");
            }

            System.out.print("Would you like to delete this file from the hard disk?(Y/N): ");
            Launcher.userInput.nextLine(); //@TODO elegant solution required
            String yesNoChoice = Launcher.userInput.nextLine();
            Launcher.userInput.close();
            if (fileToUpload.exists() && (yesNoChoice.equalsIgnoreCase("y") || yesNoChoice.equalsIgnoreCase("yes"))) {
                fileToUpload.delete();
                System.exit(0);
            } else{
                System.exit(0);
            }
        }
         catch (Exception e){
            System.out.println("An error with the upload/post-upload process has been encountered, please refer to the error and stack trace.");
            System.out.println(e.getMessage());
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    //parents is folder //true is file, false is folder
    private File driveSingleSearcher(boolean fileType, String folderName, String objName, Drive service) throws IOException{
        File foundObject = new File();
        String pageToken = null;
        String folderQuery = folderName;

        if(folderQuery.equals("")){
            folderQuery = "'root' in parents and ";
        }
        else{
            folderQuery = "'"+folderName+"' in parents and ";
        }

        if(fileType) {
            do {
                FileList result = service.files().list()
                        .setQ(folderQuery+"mimeType != 'application/vnd.google-apps.folder' and name contains '"+objName+"' and trashed = false")
                        .setFields("nextPageToken, files(name, id, parents)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
                    foundObject = file;
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        } else{
            do {
                FileList result = service.files().list()
                        .setQ(folderQuery+"mimeType = 'application/vnd.google-apps.folder' and name contains '"+objName+"' and trashed = false") //no folders, specific name text
                        .setFields("nextPageToken, files(name, id, parents)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    System.out.printf("Found folder: %s (%s)\n", file.getName(), file.getId());
                    foundObject = file;
                }
                pageToken = result.getNextPageToken();
            } while(pageToken!=null);
        }
        return foundObject;
    }

    private void driveListSearcher(boolean fileType, String folderName, Drive service) throws IOException{
        String pageToken = null;
        String folderQuery = folderName;
        FileList result;

        if(folderQuery.equals("")){
            folderQuery = "'root' in parents and ";
        }
        else{
            folderQuery = "'"+folderName+"' in parents and ";
        }

        if(fileType) {
            do {
                result = service.files().list()
                        .setQ(folderQuery+"mimeType != 'application/vnd.google-apps.folder' and trashed = false")
                        .setFields("nextPageToken, files(name, id, parents)")
                        .setPageToken(pageToken)
                        .execute();
                pageToken = result.getNextPageToken();
                for(File file:result.getFiles()){
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
                }
            } while (pageToken != null);
        } else{
            do {
                result = service.files().list()
                        .setQ(folderQuery+"mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                        .setFields("nextPageToken, files(name, id, parents)")
                        .setPageToken(pageToken)
                        .execute();
                for(File file:result.getFiles()){
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        }
    }
}
