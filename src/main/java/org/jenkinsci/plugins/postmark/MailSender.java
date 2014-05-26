package org.jenkinsci.plugins.postmark;

import com.postmark.java.NameValuePair;
import com.postmark.java.PostmarkClient;
import com.postmark.java.PostmarkException;
import com.postmark.java.PostmarkMessage;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.i18n.Messages;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ailinliu on 2014/5/26.
 */
public class MailSender extends hudson.tasks.MailSender{

    private String recipients;

    private String fromAddress;

    private String token;

    public MailSender(String recipients, boolean dontNotifyEveryUnstableBuild, boolean sendToIndividuals) {
        super(recipients, dontNotifyEveryUnstableBuild, sendToIndividuals);
    }

    public MailSender(String recipients, boolean dontNotifyEveryUnstableBuild, boolean sendToIndividuals, String fromAddress, String token) {
        super(recipients, dontNotifyEveryUnstableBuild, sendToIndividuals);
        this.recipients = recipients;
        this.fromAddress = fromAddress;
        this.token = token;
    }

    @Override
    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException {
        try {
            MimeMessage mail = getMail(build, listener);
            if (mail != null) {

                Address[] allRecipients = mail.getAllRecipients();
                if (allRecipients != null) {
                    StringBuilder buf = new StringBuilder("Sending e-mails to:");
                    for (Address a : allRecipients){
                        buf.append(' ').append(a);
                    }
                    listener.getLogger().println(buf);
                    this.send(build, mail);
//                    build.addAction(new MailMessageIdAction(mail.getMessageID()));
                } else {
                    listener.getLogger().println(Messages.MailSender_ListEmpty());
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (PostmarkException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }

        return true;
    }

    private void send(AbstractBuild<?, ?> build, javax.mail.Message mail) throws javax.mail.MessagingException, PostmarkException {
        Address[] allRecipients = mail.getAllRecipients();
        for(Address toAddress : allRecipients) {
            List<NameValuePair> headers = new ArrayList<NameValuePair>();
//            headers.add(new NameValuePair("HEADER", "test"));

            StringBuilder buf = new StringBuilder();
            appendBuildUrl(build, buf);

            PostmarkMessage message = new PostmarkMessage(fromAddress,
                    toAddress.toString(),
                    fromAddress,
                    null,
                    mail.getSubject(),
                    buf.toString(),
                    false,
                    null,
                    headers);

            PostmarkClient client = new PostmarkClient(token);
            client.sendMessage(message);
        }
    }

    private void appendBuildUrl(AbstractBuild<?, ?> build, StringBuilder buf) {
        appendUrl(Util.encode(build.getUrl())
                + (build.getChangeSet().isEmptySet() ? "" : "changes"), buf);
    }

    private void appendUrl(String url, StringBuilder buf) {
        String baseUrl = hudson.tasks.Mailer.descriptor().getUrl();
        if (baseUrl != null)
            buf.append(Messages.MailSender_Link(baseUrl, url)).append("\n\n");
    }

}
