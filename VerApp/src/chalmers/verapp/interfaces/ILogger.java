package chalmers.verapp.interfaces;

public interface ILogger {
    void Open();
    void Close();
    void WriteLine(String string);
}
