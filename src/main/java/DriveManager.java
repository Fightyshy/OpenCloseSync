import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Collections;

//text/plain, not markdown
class DriveManager {

    private final String storagePath = System.getProperty("user.dir")+java.io.File.separator+"diskStorage";

    String downloadFile(Drive service) throws IOException {
        //actual hard modo - on startup, go through every file that exists and download a fresh copy
        //@TODO Add warning feature for overwriting existing file
        File foundFile;

        System.out.print("Enter folder name to display contents: ");
        String folderName = Launcher.userInput.next();
        folderName = driveSingleSearcher("folder", "", folderName, service).getId(); //current workaround
        FileList dirList = driveListSearcher(folderName,service); //@TODO only takes ID, not name, find reason why and fix

        for(File file:dirList.getFiles()){
            System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
        }

        System.out.print("Enter file name: ");
        Launcher.userInput.nextLine();
        String searchName = Launcher.userInput.nextLine();
        Launcher.userInput.close();

        foundFile = driveSingleSearcher("file","",searchName,service);

        java.io.File dir = new java.io.File(storagePath);
        boolean mkdir = dir.mkdir();//doesn't replace if exists
        OutputStream outputStream = new FileOutputStream(new java.io.File(storagePath+java.io.File.separator+foundFile.getName()));
        service.files().get(foundFile.getId()).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return foundFile.getName();
    }

    void uploadFile(Drive service){
        //@TODO Add the actual code see comments
        //lunatic modo - extend program to run indefinitely until closed by user input, add timer to bring prog to
        //attention then ask if you want to upload it to desktop
        java.io.File[] dirList = new java.io.File(storagePath).listFiles();
        java.io.File fileToUpload = null;
        File foundFolder;
        File fileCrosscheck;
        try {
            if (dirList != null) {
                int count = 1;
                for (java.io.File file : dirList) {
                    System.out.println(Integer.toString(count) + ". " + file.getName());
                    count++;
                }
                System.out.print("Enter a file number: ");
                int fileChoice = Launcher.userInput.nextInt();
                fileToUpload = new java.io.File(storagePath+java.io.File.separator+dirList[fileChoice-1].getName());
                System.out.println(fileToUpload.getCanonicalPath());
            }
            //Look up specified folder
            System.out.print("Enter folder name to store in (blank for root): ");
            String folderName = Launcher.userInput.next();
            foundFolder = driveSingleSearcher("folder", "", folderName, service);

            //Upload to GDrive - update and create
            fileCrosscheck = driveSingleSearcher("file", foundFolder.getId(), fileToUpload!=null?fileToUpload.getName():"", service);
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
            if (fileToUpload.exists() && (yesNoChoice.equalsIgnoreCase("y") || yesNoChoice.equalsIgnoreCase("yes"))) {
                fileToUpload.delete();
            }
        }
         catch (Exception e){
            System.out.println("An error with the upload/post-upload process has been encountered, please refer to the error and stack trace.");
            System.out.println(e.getMessage());
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    //parents is folder
    private File driveSingleSearcher(String objChoice, String folderName, String objName, Drive service) throws IOException{
        File foundObject = new File();
        String pageToken = null;
        String folderQuery = folderName;

        if(folderQuery.equals("")){
            folderQuery = "";
        }
        else{
            folderQuery = "'"+folderName+"' in parents and ";
        }

        if(objChoice.equals("file")) {
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
        } else if(objChoice.equals("folder")){
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

    private FileList driveListSearcher(String folderName, Drive service) throws IOException{
        String pageToken = null;
        FileList result;
        do {
            result = service.files().list()
                    .setQ("'"+folderName+"' in parents and "+"mimeType != 'application/vnd.google-apps.folder' and trashed = false") //no folders, specific name text
                    .setFields("nextPageToken, files(name, id, parents)")
                    .setPageToken(pageToken)
                    .execute();
            pageToken = result.getNextPageToken();
        }while(pageToken!=null);
        return result;
    }

    void runProcessor(String fileName){
        try{
            System.out.println("Running markdown processor...");
//            String pathToProcessor = ""; //@TODO Platform neutrality
            Runtime.getRuntime().exec("\"C:\\Program Files\\iA Writer\\iAWriter.exe\" "+"\".\\diskStorage\\"+fileName+"\"");
        } catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to open file using document processor!");
        }
    }

    void killProcessor(){
        String line ="";
        StringBuilder PIDInfo = new StringBuilder(20);
        String wordProcessor = "iAWriter.exe";
        try{
            Process kill = Runtime.getRuntime().exec("tasklist");
            BufferedReader tasks = new BufferedReader(new InputStreamReader(kill.getInputStream()));

            do{
                PIDInfo.append(line);
                if(PIDInfo.toString().contains(wordProcessor)){
                    Runtime.getRuntime().exec("taskkill /IM iAWriter.exe /F");
                    System.out.println("Successfully killed processor.");
                    break;
                }
                if(tasks.readLine()==null){
                    System.out.println("Processor already dead, no need to kill it.");
                }
            }while((line = tasks.readLine())!=null);

//            while((line = tasks.readLine())!=null) {
//                PIDInfo += line;
//            }
////            tasks.close();
//            if(PIDInfo.contains("iAWriter.exe")){
//                Runtime.getRuntime().exec("taskkill /IM iAWriter.exe /F");
//                System.out.println("Successfully killed processor.");
//            } else{
//                System.out.println("Processor already dead, no need to kill it.");
//            }

        } catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to kill processor.");
        }
    }
}