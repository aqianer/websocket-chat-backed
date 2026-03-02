package com.example.websocketchatbacked.processer;

import java.io.File;
import java.io.IOException;

public interface FileProcesser {
    String process(File file) throws IOException;
}
