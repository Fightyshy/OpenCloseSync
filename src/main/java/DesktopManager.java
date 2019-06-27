import java.awt.*;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

class DesktopManager{

    private final String storagePath = System.getProperty("user.dir")+File.separator+"diskStorage";
    private final String userPath = System.getProperty("user.dir")+File.separator+"userData";

    //@TODO Add checks for invalid upload
    //@TODO Change to detect process being terminated //impossible(?)
    //@TODO Change to prompt to upload when process terminated
    //@TODO Add multi-program support with file association

    String openProgramPath() {
        try {
            String programPath = "";
            File[] dirList = new File(userPath).listFiles();
            if (dirList != null) {
                for (File file : dirList) {
                    if (file.getName().equals("programPath.txt")) {
                        programPath = file.getAbsolutePath();
                        break;
                    }
                }
            }
            BufferedReader reader = new BufferedReader(new FileReader (programPath));
            return reader.readLine();
        }
        catch(Exception e){
            System.out.println("programPath not found!");
        }
        return null;
    }

    //string of program path
    String setProgram(){
        String path ="";
        String exten = "";
        try{
            FileDialog dialog = new FileDialog((Frame)null, "Select program to use");
            dialog.setMode(FileDialog.LOAD);
            dialog.setDirectory(storagePath);
            dialog.setVisible(true);

            path = dialog.getDirectory()+dialog.getFile();
            exten = path.substring(path.lastIndexOf(".")+1);

            if(exten.equalsIgnoreCase("exe")){
                BufferedWriter writer =
                        new BufferedWriter(new FileWriter(userPath+File.separator+"programPath.txt"));
                writer.write(path);
                writer.close();
            }else{
                System.out.println("File selected is not a program, try again");
                path = setProgram();
            }
        }catch(IOException io){
            System.out.println("Invalid program selection! Select a proper one this time!");
        }
        return path;
    }

    void runProgram(String fileName){
            String programPath = openProgramPath();
        try{
            if(!programPath.isEmpty()){
                System.out.println("Running program...");
                Process executeProg = new ProcessBuilder(programPath,
                        storagePath+File.separator+fileName).start();
            }
            else{
                System.out.println("programPath.txt not detected, forcing selection now...");
                programPath = setProgram();

                System.out.println("Running program...");
                Process executeProg = new ProcessBuilder(programPath,
                    storagePath+File.separator+fileName).start();
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to open file using document processor!");
        }
    }

    void killProgram(){
        String line = "";
        StringBuilder PIDInfo = new StringBuilder(20);
        String programPath = openProgramPath();
        String program = "";

        try{
            Process kill = Runtime.getRuntime().exec("tasklist");
            BufferedReader tasks = new BufferedReader(new InputStreamReader(kill.getInputStream()));
            program = programPath.substring(programPath.lastIndexOf(File.separator)+1);
            do{
                PIDInfo.append(line);
                if(PIDInfo.toString().contains(program)){
                    Runtime.getRuntime().exec("taskkill "+"/IM "+"\""+program+"\""+" /F");
//                    Process closeProgram = new ProcessBuilder("cmd","/C","taskkill","/IM","\"",program,"\"","/F").start(); //working on it
                    System.out.println("Successfully killed processor.");
                    break;
                }
                else if(tasks.readLine()==null){
                    System.out.println("Processor already dead, no need to kill it.");
                }
            } while((line = tasks.readLine())!=null);
            tasks.close();
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println("Failed to kill processor.");
        }
    }
}