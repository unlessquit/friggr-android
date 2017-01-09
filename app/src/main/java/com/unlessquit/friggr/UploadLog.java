package com.unlessquit.friggr;

import java.util.List;

/**
 * Created by dezider.mesko on 19/12/2016.
 */

public class UploadLog {
//    List<UploadLogRow> rows;
    String fileName;
    String uploadDate;
    String uploadStatus;

    public String toString() {
        return String.format("%s %s %s", uploadDate, fileName, uploadStatus);
    }
}

class UploadLogRow {
    String fileName;
    String uploadDate;
    String uploadStatus;
}