package com.example.logassert;

import java.util.List;

public interface LogView {

    List<LogEntry> entries();

    LogStats stats();

    String label();
}
