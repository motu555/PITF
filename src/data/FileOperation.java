package data;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class FileOperation {
    public static String defaultEncoding = "UTF-8";

    public static String getDefaultEncoding() {
        return defaultEncoding;
    }

    public static void setDefaultEncoding(String defaultEncoding) {
        FileOperation.defaultEncoding = defaultEncoding;
    }

    public static void makeFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void makeDir(String path) {
        File file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            System.out.println(path + " dir does not exist, Create it! " + new Date());
            file.mkdir();
        } else {
            System.out.println(path + " dir exists! " + new Date());
        }

    }

    public static List<String> readAllFilePathConsRecursion(String file_path, String file_type, List<String> list) {
        File f = null;
        f = new File(file_path);
        File[] files = f.listFiles(); // 得到f文件夹下面的所有文件。
        for (File file : files) {
            if (file.isDirectory()) {
                // 如何当前路劲是文件夹，则循环读取这个文件夹下的所有文件
                FileOperation.readAllFilePathConsRecursion(file.getAbsolutePath(), file_type, list);
            } else {
                if (file.toString().endsWith("." + file_type)) {
                    list.add(file.getAbsolutePath());
                }
            }
        }
        return list;
    }

    public static List<String> readAllFilePathCons(String file_path, String file_type) {
        File f = null;
        f = new File(file_path);
        File[] files = f.listFiles(); // 得到f文件夹下面的所有文件。
        List<String> list = new ArrayList<String>();

        for (File file : files) {
            if (file.isDirectory()) {
                // 如何当前路劲是文件夹，则循环读取这个文件夹下的所有文件
                FileOperation.readAllFilePathConsRecursion(file.getAbsolutePath(), file_type, list);
            } else {
                if (file.toString().endsWith("." + file_type)) {
                    list.add(file.getAbsolutePath());
                }
            }
        }
        return list;
    }

    public static String read(String fileName, String encoding) {
        StringBuffer contentBuilder = new StringBuffer();
        String read = new String();
        FileInputStream file = null;
        BufferedReader bufferedReader = null;
        try {
            file = new FileInputStream(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(file, encoding));
            while ((read = bufferedReader.readLine()) != null) {
                contentBuilder.append(read);
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString().trim().replace("\n", "");
    }


    public static String read(String fileName) {
        return read(fileName, defaultEncoding);
    }


    public static String readAutoEncoding(String fileName) {
        return read(fileName, getEncoding(fileName));
    }

    public static ArrayList<String> readLineArrayList(String fileName, String encoding) {
        ArrayList<String> lineList = new ArrayList<String>();
        String read = new String();
        FileInputStream file = null;
        BufferedReader bufferedReader = null;
        try {
            file = new FileInputStream(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(file, encoding));
            while ((read = bufferedReader.readLine()) != null) {
                lineList.add(read.trim());
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lineList;
    }

    public static ArrayList<String> readLineArrayList(String fileName) {
        return readLineArrayList(fileName, defaultEncoding);
    }

    public static ArrayList<String> readLineArrayListAutoEncoding(String fileName) {
        return readLineArrayList(fileName, getEncoding(fileName));
    }

    public static Set<String> readLineSet(String fileName, String encoding) {
        Set<String> lineList = new HashSet<>();
        String read = new String();
        FileInputStream file = null;
        BufferedReader bufferedReader = null;
        try {
            file = new FileInputStream(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(file, encoding));
            while ((read = bufferedReader.readLine()) != null) {
                lineList.add(read.trim());
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineList;
    }

    public static Set<String> readLineSet(String fileName) {
        return readLineSet(fileName, defaultEncoding);
    }

    public static Set<String> readLineSetAutoEncodeing(String fileName) {
        return readLineSet(fileName, getEncoding(fileName));
    }

    public static String readAutoDianping(String fileName) {
        String code = getFileCharacterEnding(fileName);
        if (!code.equals("UTF-8")) {
            code = "GB2312";
        }
        return read(fileName, code);
    }

    public static void writeAppdend(String destFileName, String content, String encoding) {
        File file = new File(destFileName);
        OutputStreamWriter fileWriter;
        try {
            fileWriter = new OutputStreamWriter(new FileOutputStream(file, true), encoding);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeAppdend(String destFileName, String content) {
        writeAppdend(destFileName, content, defaultEncoding);
    }

    public static void writeNotAppdend(String destFileName, String content, String encode) {
        File file = new File(destFileName);
        OutputStreamWriter fileWriter;
        try {
            fileWriter = new OutputStreamWriter(new FileOutputStream(file, false), encode);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeNotAppdend(String destFileName, String content) {
        writeNotAppdend(destFileName, content, defaultEncoding);
    }

    public static String getEncoding(String fileName) {
        return getFileCharacterEnding(fileName);
    }

    public static String getFileCharacterEnding(String filePath) {

        File file = new File(filePath);

        return getFileCharacterEnding(file);
    }

    /**
     * Try to get file character ending.
     * </p>
     * <strong>Warning: </strong>use cpDetector to detect file's encoding.
     *
     * @param file
     * @return
     */
    public static String getFileCharacterEnding(File file) {

        String fileCharacterEnding = "UTF-8";

        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(JChardetFacade.getInstance());

        Charset charset = null;

        // File f = new File(filePath);

        try {
            charset = detector.detectCodepage(file.toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (charset != null) {
            fileCharacterEnding = charset.name();
        }

        return fileCharacterEnding;
    }
}
