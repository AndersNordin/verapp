package chalmers.verapp.ecu_connection;

public interface ILogger {
    void Open();
    void Close();
    void WriteLine(String string);
}
