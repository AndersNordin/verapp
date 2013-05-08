package chalmers.verapp.ecu_connection;

import java.io.IOException;

public interface ILogger {
    void Open();
    void Close();
    void WriteLine(String string);
}
