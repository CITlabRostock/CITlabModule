/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import eu.transkribus.core.util.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author gundram
 */
public class PythonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PythonUtil.class);

    public interface ProcessListener {

        void handleOutput(String line);

        void handleError(String line);

        void setProcessID(Long processID);
    }

    /**
     * try to find folder which is in $PYTHONPATH and check for existance of this path. Returns null otherwise.
     *
     * @return folder in PYTHONPATH, if exists or null
     */
    public static File getPythonFolder() {
        //try to find script using $PYTHONPATH as prefix
        LOG.debug("read $PYTHONPATH = '{}'", System.getenv("PYTHONPATH"));
        String property[] = System.getenv("PYTHONPATH").split(File.pathSeparator);
        File out = null;
        for (int i = 0; i < property.length; i++) {
            File folder = new File(property[i]);
            if (!folder.isDirectory() || !folder.exists()) {
                LOG.trace("cannot find folder {}", folder);
                continue;
            }
            LOG.debug("found folder {} ", folder);
            return folder;
        }
        return null;
    }

    public static int runPythonFromFile(String script, final ProcessListener listener, String... props) {
        final Logger LOGPYTHON = LoggerFactory.getLogger("resources." + script.replace(File.separatorChar, '.'));
        ArrayList<String> cmd = new ArrayList<>();
        File execFolder = new File(".");
        File execScript = new File(script);
        if (!execScript.exists()) {
            LOG.debug("script {} does not exist directly, try to read $PYTHONPATH for resolve position...", script);
            File pythonFolder = getPythonFolder();
            File folder = pythonFolder.getParentFile();
            if (!folder.isDirectory() || !folder.exists()) {
                throw new RuntimeException("cannot find folder " + folder);
            }
            File script2 = new File(folder, script);
            if (!script2.isFile() || !script2.exists()) {
                throw new RuntimeException("cannot find file " + script);
            }
            execFolder = folder;
//                script = script2;
            LOG.debug("found script {} and program will be run from folder {}", script, execFolder);
        }
        LOGPYTHON.debug("start python script '{}'", script);
        cmd.add("python");
        cmd.add("-u");  //unbuffered output   
        cmd.add(new File(execFolder, script).getAbsolutePath());
//        cmd.add("--jars=/home/gundram/devel/src/git/python/tf_htsr/util/loader/libs/reco_lab");
        for (int i = 0; i < props.length; i += 2) {
            cmd.add("--" + props[i] + "=" + props[i + 1]);
        }
        StringBuilder sb = new StringBuilder();
        for (String string : cmd) {
            sb.append(' ').append(string);
        }
        LOG.info(sb.toString());
//        System.out.println(sb.toString().trim());
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(execFolder);
        try {
            Process p = pb.start();
            Long aLong = SysUtils.processId(p);
            if (aLong != null) {
                listener.setProcessID(aLong);
            }
            final BufferedReader errMsg = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            BufferedReader outMsg = new BufferedReader(new InputStreamReader(p.getInputStream()));
            ExecutorService pool = Executors.newFixedThreadPool(2, new ThreadFactory() {
                int i = 0;

                @Override
                public Thread newThread(Runnable r) {
                    i++;
                    return new Thread(r, i == 1 ? "out" : i == 2 ? "err" : "NONE");
                }
            });
            pool.submit(new Runnable() {
                String line = "";
                private final Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();

                @Override
                public void run() {
                    try {
                        if (mdcContextMap != null && !mdcContextMap.isEmpty()) {
                            MDC.setContextMap(mdcContextMap);
                        }
                    } catch (RuntimeException ex) {
                        LOGPYTHON.error("error occures in setting up logging", ex);
                    }
                    try {
                        while ((line = outMsg.readLine()) != null) {
                            LOGPYTHON.debug(line);
                            if (listener != null) {
                                try {
                                    listener.handleOutput(line);
                                } catch (Throwable ex) {
                                    LOGPYTHON.warn("error occures in class {} ", listener.getClass().getName(), ex);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGPYTHON.error("error occures in OutputStream", ex);
                    }
                }

            });
            pool.submit(new Runnable() {
                String line = "";
                private final Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();

                @Override
                public void run() {
                    try {
                        if (mdcContextMap != null && !mdcContextMap.isEmpty()) {
                            MDC.setContextMap(mdcContextMap);
                        }
                    } catch (RuntimeException ex) {
                        LOGPYTHON.error("error occures in setting up logging", ex);
                    }
                    try {
                        while ((line = errMsg.readLine()) != null) {
                            LOGPYTHON.error(line);
                            if (listener != null) {
                                try {
                                    listener.handleError(line);
                                } catch (Throwable ex) {
                                    LOGPYTHON.warn("error occures in class {} ", listener.getClass().getName(), ex);
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        LOGPYTHON.error("error occures in ErrorStream", ex);
                    }
                }
            });
            pool.shutdown();
            int res;
            try {
                res = p.waitFor();
            } catch (InterruptedException ex) {
                LOGPYTHON.error("wait for process fails", ex);
                p.destroyForcibly();
                res = 1;
            }
            if (res != 0) {
                LOGPYTHON.error("end python script '{}' with return value = {}", script, res);
            } else {
                LOGPYTHON.info("end python script '{}' with return value = 0 ", script);
            }
            p.destroy();
            return res;
        } catch (IOException ex) {
            LOGPYTHON.error("Cannot execute '" + script + "'.");
            throw new RuntimeException("Cannot execute '" + script + "'.", ex);
        }
    }

    public static int runPythonFromResource(String script, final ProcessListener listener, String... props) {
        if (script == null) {
            throw new RuntimeException("no script is given");
        }
//        Map<String, String> env = System.getenv();
//        for (String object : env.keySet()) {
//            System.out.println(object + "=>" + env.get(object));
//        }
        File resourceAsFile = getResourceAsFile(script);
        return runPythonFromFile(resourceAsFile.getAbsolutePath(), listener, props);
//        cmd.add("PYTHONPATH=\"/home/gundram/devel/src/git/python/tf_htsr\"");
    }

    public static File getResourceAsFile(String resourcePath) {
        try {
            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                return null;
            }
            String suffix = ".tmp";
            int idx = resourcePath.lastIndexOf('.');
            if (idx >= 0) {
                suffix = resourcePath.substring(idx);
                resourcePath = resourcePath.substring(0, idx);
            }
            File tempFile = File.createTempFile(resourcePath.replace(File.separator, "_"), suffix);
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                //copy stream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
