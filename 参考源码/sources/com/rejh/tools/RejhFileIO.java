package com.rejh.tools;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;

/* JADX INFO: loaded from: classes.dex */
public class RejhFileIO {
    private String APPTAG;
    private Context context;

    public RejhFileIO(Context context, String str) {
        this.APPTAG = "FileIO";
        this.context = context;
        this.APPTAG = str;
    }

    public boolean cacheInputStream(String str, InputStream inputStream) throws Throwable {
        FileOutputStream fileOutputStream = null;
        try {
            try {
                FileOutputStream fileOutputStream2 = new FileOutputStream(new File(this.context.getCacheDir(), str));
                try {
                    byte[] bArr = new byte[1024];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i > 0) {
                            fileOutputStream2.write(bArr, 0, i);
                        } else {
                            try {
                                fileOutputStream2.close();
                                return true;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }
                } catch (Exception e2) {
                    e = e2;
                    fileOutputStream = fileOutputStream2;
                    e.printStackTrace();
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                            return true;
                        } catch (IOException e3) {
                            e3.printStackTrace();
                            return false;
                        }
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    fileOutputStream = fileOutputStream2;
                    if (fileOutputStream == null) {
                        return false;
                    }
                    try {
                        fileOutputStream.close();
                        throw th;
                    } catch (IOException e4) {
                        e4.printStackTrace();
                        return false;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e5) {
            e = e5;
        }
    }

    public boolean writeInputStream(String str, InputStream inputStream) {
        return writeInputStream(str, inputStream, 0);
    }

    public boolean writeInputStream(String str, InputStream inputStream, int i) {
        FileOutputStream fileOutputStreamOpenFileOutput = null;
        try {
            try {
                fileOutputStreamOpenFileOutput = this.context.openFileOutput(str, i);
                byte[] bArr = new byte[1024];
                while (true) {
                    int i2 = inputStream.read(bArr);
                    if (i2 <= 0) {
                        break;
                    }
                    fileOutputStreamOpenFileOutput.write(bArr, 0, i2);
                }
                if (fileOutputStreamOpenFileOutput != null) {
                    try {
                        fileOutputStreamOpenFileOutput.close();
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            } catch (Exception e2) {
                e2.printStackTrace();
                if (fileOutputStreamOpenFileOutput != null) {
                    try {
                        fileOutputStreamOpenFileOutput.close();
                        return true;
                    } catch (IOException e3) {
                        e3.printStackTrace();
                        return false;
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (fileOutputStreamOpenFileOutput == null) {
                return false;
            }
            try {
                fileOutputStreamOpenFileOutput.close();
                throw th;
            } catch (IOException e4) {
                e4.printStackTrace();
                return false;
            }
        }
    }

    public boolean writeTextFile(String str, String str2) {
        return writeTextFile(str, str2, 0);
    }

    public boolean writeTextFile(String str, String str2, int i) {
        FileOutputStream fileOutputStreamOpenFileOutput = null;
        try {
            try {
                fileOutputStreamOpenFileOutput = this.context.openFileOutput(str, i);
                fileOutputStreamOpenFileOutput.write(str2.getBytes());
                if (fileOutputStreamOpenFileOutput != null) {
                    try {
                        fileOutputStreamOpenFileOutput.close();
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            } catch (Throwable th) {
                if (fileOutputStreamOpenFileOutput == null) {
                    return false;
                }
                try {
                    fileOutputStreamOpenFileOutput.close();
                    throw th;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (fileOutputStreamOpenFileOutput != null) {
                try {
                    fileOutputStreamOpenFileOutput.close();
                    return true;
                } catch (Exception e4) {
                    e4.printStackTrace();
                    return false;
                }
            }
            return false;
        }
    }

    public String readTextFile(String str) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(this.context.getFilesDir(), str)));
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                    sb.append('\n');
                } else {
                    bufferedReader.close();
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeJsonsArrayFile(String str, String str2, String str3) {
        String[] strArrSplit = str2.split(str3);
        JSONArray jSONArray = new JSONArray();
        for (String str4 : strArrSplit) {
            jSONArray.put(str4);
        }
        writeTextFile(str, jSONArray.toString());
    }

    public void writeJsonsArrayFile(String str, JSONArray jSONArray) {
        writeTextFile(str, jSONArray.toString());
    }

    public JSONArray readJsonArrayFile(String str) {
        JSONArray jSONArray = new JSONArray();
        try {
            String textFile = readTextFile(str);
            if (textFile == null) {
                textFile = "[]";
            }
            return new JSONArray(textFile);
        } catch (JSONException e) {
            e.printStackTrace();
            return jSONArray;
        }
    }
}
