import java.awt.*;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

class DesktopManager{

    private final String storagePath = System.getProperty("user.dir")+java.io.File.separator+"diskStorage";
    private final String userPath = System.getProperty("user.dir")+java.io.File.separator+"userData";


    void runProgram(String fileName){
        try{
            String programPath = "";
            java.io.File[] dirList = new java.io.File(userPath).listFiles();
            if(dirList != null){
                for(java.io.File file : dirList){
                    if(file.getName().equals("processorDirectory.txt")){
                        programPath = file.getAbsolutePath();
                        break;
                    }
                }
            }

            if(!programPath.isEmpty()){
                BufferedReader reader = new BufferedReader(new FileReader (programPath));
                System.out.println("Running program...");
                Runtime.getRuntime().exec("\""+reader.readLine()+"\" "+"\".\\diskStorage\\"+fileName+"\"");
                reader.close();
            }
            else{
                FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
                dialog.setMode(FileDialog.LOAD);
                dialog.setDirectory(storagePath);
                dialog.setVisible(true);
                programPath = dialog.getDirectory()+dialog.getFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(userPath+java.io.File.separator+"processorDirectory.txt"));
                writer.write(programPath);
                writer.close();

                System.out.println("Running markdown processor...");
                //@TODO Platform neutrality
                Runtime.getRuntime().exec("\""+programPath+"\" "+"\".\\diskStorage\\"+fileName+"\"");
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to open file using document processor!");
        }
    }

    void killProgram(){
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
                else if(tasks.readLine()==null){
                    System.out.println("Processor already dead, no need to kill it.");
                }
            } while((line = tasks.readLine())!=null);
        }

        catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to kill processor.");
        }
    }
}