import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.sun.org.apache.bcel.internal.generic.LUSHR;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Collections;

//text/plain, not markdown
public class DriveManager {
    private static final String downloadDir = "./diskStorage/";

    public static String downloadFile(Drive service) throws IOException {
        //actual hard modo - on startup, go through every file that exists and download a fresh copy
        //@TODO Add warning feature for overwriting existing file
        String fileName = "";
        File foundFile = new File();

        System.out.print("Enter file name: ");
        String searchName = Launcher.userInput.next();
        Launcher.userInput.close();

        foundFile = driveSearcher("file","",searchName,service);

        java.io.File dir = new java.io.File(downloadDir);
        dir.mkdir(); //doesn't replace if exists
        OutputStream outputStream = new FileOutputStream(new java.io.File(downloadDir+foundFile.getName()));
        service.files().get(foundFile.getId()).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return foundFile.getName();
    }

    public static void uploadFile(Drive service) throws IOException{
        //@TODO Add the actual code see comments
        java.io.File dir = new java.io.File(".\\diskStorage");
        java.io.File[] dirList = dir.listFiles();
        java.io.File fileToUpload = null;
        File foundFolder = new File();
        File fileCrosscheck = new File();

        if(dirList!=null){
            int count = 1;
            for(java.io.File file : dirList){
                System.out.println(Integer.toString(count)+". "+file.getName());
                count++;
            }
            System.out.print("Enter a file number: ");
            int fileChoice = Launcher.userInput.nextInt();
            fileToUpload = new java.io.File(dir.getName()+"\\"+dirList[fileChoice-1].getName());
            System.out.println(fileToUpload.getCanonicalPath());
        }
        //Look up specified folder
        System.out.print("Enter folder name to store in (blank for root): ");
        String folderName = Launcher.userInput.next();
        foundFolder = driveSearcher("folder","",folderName,service);
        Launcher.userInput.close();

        //Upload to GDrive - update and create
        fileCrosscheck = driveSearcher("file",foundFolder.getId(),fileToUpload.getName(),service);
        if(fileCrosscheck.getName() != null){
            FileContent toUpdate = new FileContent("text/plain",fileToUpload);
            File content = new File();
            content.setName(fileToUpload.getName());
            content.setMimeType("text/plain");
            service.files().update(fileCrosscheck.getId(),content,toUpdate).execute();
            System.out.println("Overwritten old file");
        } else{
            File metadata = new File();
            metadata.setName(fileToUpload.getName());
            metadata.setMimeType("text/plain");
            metadata.setParents(Collections.singletonList(foundFolder.getId()));
            FileContent content = new FileContent("text/plain",fileToUpload);
            service.files().create(metadata,content).setFields("id, parents").execute();
            System.out.println("No old file was found so a new one was created");
        }

        //Double check lookup
        try{
            File syncCheck = driveSearcher("file",foundFolder.getId(),fileToUpload.getName(),service);
            System.out.println("Upload confirmed successful");
        } catch(Exception e){
            System.out.println("File not found, upload probably had issues");
            System.out.println(e.getMessage());
        }
        //if successful, delete hard disk copy, alternatively add option to keep it
        //lunatic modo - extend program to run indefinitely until closed by user input, add timer to bring prog to
        //attention then ask if you want to upload it to desktop
    }

    //parents is folder
    public static File driveSearcher(String objChoice, String folderName, String objName, Drive service) throws IOException{
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
                    System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
                    foundObject = file;
                }
                pageToken = result.getNextPageToken();
            } while(pageToken!=null);
        }
        return foundObject;
    }

    public static void runProcessor(String fileName){
        try{
            System.out.println("Running markdown processor...");
            System.out.println("\"C:\\Program Files\\iA Writer\\iAWriter.exe\" "+"\".\\diskStorage\\"+fileName+"\"");
            Runtime.getRuntime().exec("\"C:\\Program Files\\iA Writer\\iAWriter.exe\" "+"\".\\diskStorage\\"+fileName+"\"");
        } catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to open file using document processor!");
        }
    }

    public static void killProcessor(){
        String line;
        String PIDInfo = "";
        try{
            Process kill = Runtime.getRuntime().exec("tasklist");
            BufferedReader tasks = new BufferedReader(new InputStreamReader(kill.getInputStream()));

            while((line = tasks.readLine())!=null) {
                PIDInfo += line;
            }
            tasks.close();
            if(PIDInfo.contains("iAWriter.exe")){
                Runtime.getRuntime().exec("taskkill /IM iAWriter.exe /F");
                System.out.println("Successfully killed processor.");
            } else{
                System.out.println("Processor already dead, no need to kill it.");
            }

        } catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to kill processor.");
        }
    }
}
