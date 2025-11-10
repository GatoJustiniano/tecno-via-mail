package Servicios;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class SmtpService {

    private final String SERVER = "mail.tecnoweb.org.bo";
    private final String USER_EMISOR = "grupo03sc@tecnoweb.org.bo";
    private final int PUERTO = 25;

    String line;
    String comando = "";

    public boolean sendEmail(String mensaje, String receptor) {
    // Asegurar que el mensaje tenga el formato correcto, eliminando `\r` solitarios y normalizando a `\r\n`
    mensaje = createHTML(mensaje)
                .replace("\r\n", "\n") // Eliminar retornos de carro redundantes
                .replace("\r", "")     // Eliminar cualquier `\r` suelto
                .replace("\n", "\r\n"); // Convertir saltos de línea a `\r\n`

    try {
        Socket socket = new Socket(SERVER, PUERTO);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream salida = new DataOutputStream(socket.getOutputStream());

        if (socket != null && entrada != null && salida != null) {
            // Enviar comandos SMTP y el mensaje
            if (!initializeSmtpSession(entrada, salida, receptor)) return false;

            String comando = "DATA\r\n";
            salida.writeBytes(comando);
            String line = entrada.readLine();
            if (!line.startsWith("354")) {
                throw new RuntimeException("Error en el comando DATA: " + line);
            }

            // Enviar el cuerpo del mensaje asegurando que las líneas terminen correctamente
            comando = "Subject: Respuesta a consulta\r\n";
            comando += "Content-Type: text/html; charset=UTF-8\r\n";
            comando += "\r\n" + mensaje + "\r\n.\r\n";
            salida.writeBytes(comando);

            line = entrada.readLine();
            if (!line.startsWith("250")) {
                throw new RuntimeException("Error al enviar el mensaje: " + line);
            }

            finalizarSmtp(salida, entrada);
            return true;
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}


    public boolean sendEmailError(String subject, String body, String receptor) {
        body = createHTML(body);
        try {
            Socket socket = new Socket(SERVER, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());

            if (socket != null && entrada != null && salida != null) {
                // Enviar comandos SMTP y el mensaje
                if (!initializeSmtpSession(entrada, salida, receptor)) return false;

                comando = "DATA\r\n";
                salida.writeBytes(comando);
                line = entrada.readLine();
                if (!line.startsWith("354")) {
                    throw new RuntimeException("Error en el comando DATA: " + line);
                }

                // Enviar el cuerpo del mensaje con el asunto
                comando = "Subject: " + subject + "\r\n";
                comando += "Content-Type: text/html; charset=UTF-8\r\n";
                comando += "\r\n" + body + "\r\n.\r\n";
                salida.writeBytes(comando);
                line = entrada.readLine();
                if (!line.startsWith("250")) {
                    throw new RuntimeException("Error al enviar el mensaje: " + line);
                }

                finalizarSmtp(salida, entrada);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean initializeSmtpSession(BufferedReader entrada, DataOutputStream salida, String receptor) throws Exception {
        // Leer la respuesta de bienvenida
        line = entrada.readLine();
        if (!line.startsWith("220")) {
            throw new RuntimeException("Error al conectar al servidor SMTP: " + line);
        }

        // Enviar comandos SMTP iniciales
        comando = "HELO " + SERVER + "\r\n";
        salida.writeBytes(comando);
        line = entrada.readLine();
        if (!line.startsWith("250")) {
            throw new RuntimeException("Error en el comando HELO: " + line);
        }

        comando = "MAIL FROM:<" + USER_EMISOR + ">\r\n";
        salida.writeBytes(comando);
        line = entrada.readLine();
        if (!line.startsWith("250")) {
            throw new RuntimeException("Error en el comando MAIL FROM: " + line);
        }

        comando = "RCPT TO:<" + receptor + ">\r\n";
        salida.writeBytes(comando);
        line = entrada.readLine();
        if (!line.startsWith("250")) {
            throw new RuntimeException("Error en el comando RCPT TO: " + line);
        }
        return true;
    }

    private void finalizarSmtp(DataOutputStream salida, BufferedReader entrada) throws Exception {
        // Finalizar la sesión SMTP
        comando = "QUIT\r\n";
        salida.writeBytes(comando);
        line = entrada.readLine();
        if (!line.startsWith("221")) {
            throw new RuntimeException("Error en el comando QUIT: " + line);
        }
    }

    private String createHTML(String mensaje) {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"es\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
               "</head>\n" +
               "<body>\n" +
               "    <p>" + mensaje + "</p>\n" +
               "</body>\n" +
               "</html>";
    }

}
