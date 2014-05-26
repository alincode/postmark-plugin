package org.jenkinsci.plugins.postmark;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * @author AiLin Liou
 */
public class Mailer extends Notifier {

    protected static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

    private final String recipients;

    @DataBoundConstructor
    public Mailer(String recipients) {
        this.recipients = recipients;
    }

    public String getRecipients() {
        return recipients;
    }

    public static DescriptorImpl descriptor() {
        return Jenkins.getInstance().getDescriptorByType(Mailer.DescriptorImpl.class);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException {

        String fromAddress = getDescriptor().getSender();
        String token = getDescriptor().getToken();

        if(isEmpty(fromAddress)){
            listener.error(formatErrorMessage("fromAddress"));
            return true;
        }

        if(isEmpty(recipients)){
            listener.error(formatErrorMessage("recipients"));
            return true;
        }

        if(isEmpty(token)){
            listener.error(formatErrorMessage("api token"));
            return true;
        }

        boolean dontNotifyEveryUnstableBuild = false;
        boolean sendToIndividuals = false;

        return new MailSender(recipients, dontNotifyEveryUnstableBuild, sendToIndividuals, fromAddress, token){}.execute(build,listener);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    private String formatErrorMessage(String filed){
        return String.format("%s not configured; cannot send mail notification", filed);
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class
                .getName());

        public String sender;

        public String token;

        public DescriptorImpl() {
            super(Mailer.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            sender = formData.getString("sender");
            token = formData.getString("token");
            save();
            return super.configure(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Postmark Email Notifier";
        }

        public String getSender(){
            return sender;
        }

        public String getToken() {
            return token;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }
}