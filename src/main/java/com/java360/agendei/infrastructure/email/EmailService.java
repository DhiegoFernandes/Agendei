package com.java360.agendei.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente; // o e-mail configurado no application.yml

    public void enviarCodigoRecuperacao(String email, String codigo) {
        try {
            String assunto = "Recuperação de Senha - Agendei";
            String link = "https://agendei.com.br/redefinir-senha?email=" + email + "&codigo=" + codigo;

            String html = """
                    <div style="font-family: Arial, sans-serif; background-color:#f9f9f9; padding:20px;">
                        <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                            <div style="text-align:center; margin-bottom:30px;">
                                <img src="https://i.imgur.com/6xgYbBt.png" alt="Logo Agendei" width="160"/>
                            </div>
                            <h2 style="color:#333;">Olá!</h2>
                            <p style="font-size:16px; color:#555;">
                                Recebemos uma solicitação para redefinir sua senha no <strong>Agendei</strong>.
                            </p>
                            <p style="font-size:16px; color:#555;">
                                Seu código de verificação é:
                            </p>
                            <div style="text-align:center; margin:20px 0;">
                                <span style="font-size:28px; font-weight:bold; letter-spacing:5px; color:#1E88E5;">%s</span>
                            </div>
                            <p style="font-size:16px; color:#555;">
                                Ou clique no botão abaixo para redefinir sua senha diretamente:
                            </p>
                            <div style="text-align:center; margin:30px 0;">
                                <a href="%s" style="background-color:#1E88E5; color:white; text-decoration:none; padding:12px 24px; border-radius:5px; font-size:16px;">
                                    Redefinir Senha
                                </a>
                            </div>
                            <p style="font-size:14px; color:#999;">
                                Se você não solicitou a recuperação, ignore este e-mail.
                            </p>
                        </div>
                        <div style="text-align:center; margin-top:20px; font-size:12px; color:#aaa;">
                            © 2025 Agendei - Todos os direitos reservados.
                        </div>
                    </div>
                    """.formatted(codigo, link);

            enviarEmailHtml(email, assunto, html);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage());
        }
    }

    private void enviarEmailHtml(String destinatario, String assunto, String conteudoHtml) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinatario);
        helper.setFrom(remetente, "Agendei"); // nome visível no e-mail
        helper.setSubject(assunto);
        helper.setText(conteudoHtml, true); // true = HTML

        mailSender.send(message);
    }
    }