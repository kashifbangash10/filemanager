
package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.root;
import android.util.Log;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootCommands {

    private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";
    private static SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static String getCommandLineString(String input) {
        return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
    }

    public static InputStream getFile(String path) {
        InputStream in = null;

        try {
            in = openFile("cat " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static InputStream putFile(String path, String text) {
        InputStream in = null;

        try {
            in = openFile("echo \"" + text + "\" > " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public static ArrayList<String> listFiles(String path) {
        ArrayList<String> listFiles = new ArrayList<>();

        try {
            listFiles = execute("ls -l " + getCommandLineString(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listFiles;
    }

    public static ArrayList<String> listFiles(String path, boolean showhidden) {
        ArrayList<String> mDirContent = new ArrayList<>();

        ArrayList<String> listFiles = execute("ls -a " + getCommandLineString(path));
        for (String line : listFiles){
            if (!showhidden) {
                if (line.charAt(0) != '.')
                    mDirContent.add(path + "/" + line);
            } else {
                mDirContent.add(path + "/" + line);
            }
        }

        return mDirContent;
    }

    public static ArrayList<String> findFiles(String path, String query) {
        ArrayList<String> listFiles = new ArrayList<>();

        try {
            listFiles =  execute("find " + getCommandLineString(path) + " -type f -iname " + '*' + getCommandLineString(query) + '*' + " -exec ls -ls {} \\;");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listFiles;
    }

    public static ArrayList<String> findFile(String path, String query) {
        ArrayList<String> mDirContent = new ArrayList<>();

        try {
            mDirContent = execute("find " + getCommandLineString(path) + " -type f -iname " + '*' + getCommandLineString(query) + '*' + " -exec ls -a {} \\;");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    
    public static boolean createRootdir(String parentPath, String name) {
        File dir = new File(parentPath + File.separator + name);
        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(parentPath, "rw");

            execute("mkdir " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    
    public static boolean createRootFile(String parentPath, String name) {
        File dir = new File(parentPath + File.separator + name);

        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(parentPath, "rw");

            execute("touch " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    
    public static boolean moveCopyRoot(String old, String newDir) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(newDir, "rw");

            execute("cp -fr " + getCommandLineString(old) + " "
                    + getCommandLineString(newDir));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    
    
    
    public static boolean renameRootTarget(String path, String oldname, String name) {
        File file = new File(path + File.separator + oldname);
        File newf = new File(path + File.separator + name);

        if (name.length() < 1)
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            execute("mv " + getCommandLineString(file.getAbsolutePath()) + " "
                    + getCommandLineString(newf.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    
    
    
    public static boolean renameRootTarget(RootFile before, RootFile after) {
        File file = new File(before.getParent() + File.separator + before.getName());
        File newf = new File(after.getParent() + File.separator + after.getName());

        if (after.getName().length() < 1)
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(before.getPath(), "rw");

            execute("mv " + getCommandLineString(file.getAbsolutePath()) + " "
                    + getCommandLineString(newf.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    
    public static boolean deleteFileRoot(String path) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            if (new File(path).isDirectory()) {
                execute("rm -f -r " + getCommandLineString(path));
            } else {
                execute("rm -r " + getCommandLineString(path));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    
    private static boolean readReadWriteFile() {
        File mountFile = new File("/proc/mounts");
        StringBuilder procData = new StringBuilder();
        if (mountFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(mountFile.toString());
                DataInputStream dis = new DataInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        dis));
                String data;
                while ((data = br.readLine()) != null) {
                    procData.append(data).append("\n");
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            String[] tmp = procData.toString().split("\n");
            for (String aTmp : tmp) {
                
                
                if (aTmp.contains("/dev/block")
                        && aTmp.contains("/system")) {
                    if (aTmp.contains("rw")) {
                        
                        return true;
                    } else if (aTmp.contains("ro")) {
                        
                        return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsIllegals(String toExamine) {
        
        
        Pattern pattern = Pattern.compile("[+]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    private static synchronized ArrayList<String> execute(String cmd){
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ArrayList<String> list = new ArrayList<>();
        final AtomicReference<ArrayList<String>> resultRef = new AtomicReference<>();
        Command command = new Command(0, cmd) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                list.add(line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
                super.commandTerminated(id, reason);
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                super.commandCompleted(id, exitcode);
                resultRef.set(list);
                countDownLatch.countDown();
            }
        };
        try {
            RootTools.getShell(true).add(command);
            countDownLatch.await();
        } catch (IOException | RootDeniedException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
        return resultRef.get();
    }

    private static InputStream openFile(String cmd) {
        InputStream inputStream;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(
                    process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            inputStream = process.getInputStream();
            String err = (new BufferedReader(new InputStreamReader(
                    process.getErrorStream()))).readLine();
            os.flush();

            if (process.waitFor() != 0 || (!"".equals(err) && null != err)
                    && !containsIllegals(err)) {
                Log.e("Root Error, cmd: " + cmd, err);
                return null;
            }
            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean changeGroupOwner(File file, String owner, String group) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(file.getAbsolutePath(), "rw");

            execute("chown " + owner + "." + group + " "
                    + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean applyPermissions(File file, Permissions permissions) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(file.getAbsolutePath(), "rw");

            execute("chmod " + Permissions.toOctalPermission(permissions) + " "
                    + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String[] getFileProperties(File file) {
        String[] info = null;

        try {
            ArrayList<String> listFiles = execute("ls -l "
                    + getCommandLineString(file.getAbsolutePath()));
            for (String line : listFiles){
                info = getAttrs(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    private static String[] getAttrs(String string) {
        if (string.length() < 44) {
            throw new IllegalArgumentException("Bad ls -l output: " + string);
        }
        final char[] chars = string.toCharArray();

        final String[] results = new String[11];
        int ind = 0;
        final StringBuilder current = new StringBuilder();

        Loop:
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                case '\t':
                    if (current.length() != 0) {
                        results[ind] = current.toString();
                        ind++;
                        current.setLength(0);
                        if (ind == 10) {
                            results[ind] = string.substring(i).trim();
                            break Loop;
                        }
                    }
                    break;

                default:
                    current.append(chars[i]);
                    break;
            }
        }

        return results;
    }

    public static long getTimeinMillis(String date){
        long timeInMillis = 0;
        try {
            timeInMillis = simpledateformat.parse(date).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timeInMillis;
    }
}