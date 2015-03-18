package org.xwiki.contrib.application.task.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class TaskManagerViewPage extends ViewPage
{
    private static final String CLASS_PREFIX = "TaskManager.TaskManagerClass_0_";
    
    @FindBy(xpath = "//div[contains(@id, \"document-title\")]/h1")
    private WebElement nameElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "severity\"]/../../dd/p")
    private WebElement severityElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "assignee\"]/../../dd/ul/li/div[contains(@class, \"user-name\")]")
    private WebElement assigneeElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "status\"]/../../dd/p")
    private WebElement statusElement;
    
    @FindBy(xpath = "//label[@for=\"" + CLASS_PREFIX + "progress\"]/../../dd/div/div/span")
    private WebElement progressElement;
    
    /**
     * Opens the home page.
     */
    public static TaskManagerViewPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page);
        return new TaskManagerViewPage();
    }
    
    public String getName() {
    	return nameElement.getText();
    }
    
    public String getSeverity() {
    	return severityElement.getText();
    }
    
    public String getAssignee() {
    	return assigneeElement.getText();
    }
    
    public String getStatus() {
    	return statusElement.getText();
    }
    
    public String getProgress() {
    	return progressElement.getText();
    }
}
