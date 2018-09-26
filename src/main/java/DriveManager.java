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

    String downloadFile(Drive service) throws IOException {
        //actual hard modo - on startup, go through every file that exists and download a fresh copy
        //@TODO Add warning feature for overwriting existing file
        File foundFile;

        System.out.print("Enter folder name to display contents: ");
        String folderName = Launcher.userInput.next();
        folderName = driveSingleSearcher("folder", "", folderName, service).getId(); //current workaround
        driveListSearcher(true,folderName,service); //@TODO only takes ID, not name, find reason why and fix

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
        java.io.File[] dirList = new java.io.File(storagePath).listFiles();
        java.io.File fileToUpload = null;
        File foundFolder;
        File fileCrosscheck;
        try {
//            if (dirList != null) {
//                int count = 1;
//                for (java.io.File file : dirList) {
//                    System.out.println(Integer.toString(count) + ". " + file.getName());
//                    count++;
//                }
//                System.out.print("Enter a file number: ");
//                int fileChoice = Launcher.userInput.nextInt();
//                fileToUpload = new java.io.File(storagePath+java.io.File.separator+dirList[fileChoice-1].getName());
//            }
            FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
            dialog.setMode(FileDialog.LOAD);
            dialog.setDirectory(storagePath);
            dialog.setVisible(true);
            java.io.File[] file = dialog.getFiles();
            fileToUpload = new java.io.File(storagePath+java.io.File.separator+file[0].getName());

            System.out.println("Folders found below:");
            driveListSearcher(false,"root", service);
            System.out.println();
            System.out.print("Enter folder name to store in (blank for root): ");
            String folderName = Launcher.userInput.next();
            foundFolder = driveSingleSearcher("folder", "", folderName, service);
            System.out.println();

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

    private void driveListSearcher(boolean fileType, String folderName, Drive service) throws IOException{
        String pageToken = null;
        FileList result;
        if(fileType) {
            do {
                result = service.files().list()
                        .setQ("'" + folderName + "' in parents and " + "mimeType != 'application/vnd.google-apps.folder' and trashed = false") //no folders, specific name text
                        .setFields("nextPageToken, files(name, id, parents)")
                        .setPageToken(pageToken)
                        .execute();
                pageToken = result.getNextPageToken();
                for(File file:result.getFiles()){
                    System.out.printf("%s (%s)\n", file.getName(), file.getId());
                }
            } while (pageToken != null);
        } else if(!fileType){
            do {
                result = service.files().list()
                        .setQ("'" + folderName + "' in parents and " + "mimeType = 'application/vnd.google-apps.folder' and trashed = false") //no folders, specific name text
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

    void runProcessor(String fileName){
        try {
            String processorPath = "";
            java.io.File[] dirList = new java.io.File(userPath).listFiles();
                if (dirList != null) {
                    for (java.io.File file : dirList) {
                        if(file.getName().equals("processorDirectory.txt")){
                            processorPath = file.getAbsolutePath();
                            break;
                        }
                    }
                }
            if(!processorPath.isEmpty()){
                BufferedReader reader = new BufferedReader(new FileReader(processorPath));
                System.out.println("Running markdown processor...");
                //@TODO Platform neutrality
                Runtime.getRuntime().exec("\""+reader.readLine()+"\" "+"\".\\diskStorage\\"+fileName+"\"");
                reader.close();
            }else{
                    FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
                    dialog.setMode(FileDialog.LOAD);
                    dialog.setDirectory(storagePath);
                    dialog.setVisible(true);
                    processorPath = dialog.getDirectory()+dialog.getFile();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(userPath+java.io.File.separator+"processorDirectory.txt"));
                    writer.write(processorPath);
                    writer.close();

                    System.out.println("Running markdown processor...");
                    //@TODO Platform neutrality
                    Runtime.getRuntime().exec("\""+processorPath+"\" "+"\".\\diskStorage\\"+fileName+"\"");
                }
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
