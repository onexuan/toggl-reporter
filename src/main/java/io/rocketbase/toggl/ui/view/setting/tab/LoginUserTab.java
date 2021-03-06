package io.rocketbase.toggl.ui.view.setting.tab;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import io.rocketbase.toggl.backend.security.MongoUserDetails;
import io.rocketbase.toggl.backend.security.MongoUserService;
import io.rocketbase.toggl.backend.security.UserRole;
import io.rocketbase.toggl.ui.component.tab.AbstractTab;
import io.rocketbase.toggl.ui.view.setting.form.MongoUserForm;
import org.vaadin.viritin.MSize;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.vaadin.viritin.v7.fields.MPasswordField;
import org.vaadin.viritin.v7.fields.MTable;
import org.vaadin.viritin.v7.form.AbstractForm.DeleteHandler;
import org.vaadin.viritin.v7.form.AbstractForm.SavedHandler;

import javax.annotation.Resource;

/**
 * Created by marten on 09.03.17.
 */
@UIScope
@SpringComponent
public class LoginUserTab extends AbstractTab {

    @Resource
    private MongoUserService mongoUserService;

    private MTable<MongoUserDetails> userTable;

    @Override
    public Component initLayout() {
        userTable = initUserTable();

        return new MVerticalLayout()
                .add(new MButton(VaadinIcons.PLUS, "add", e -> {
                    new MongoUserForm(MongoUserDetails.builder()
                            .role(UserRole.ROLE_USER)
                            .build())
                            .initCreateWindow((SavedHandler<MongoUserDetails>) entity -> {
                                mongoUserService.register(entity);
                            })
                            .addCloseListener(closeEvent -> {
                                refreshTab();
                            });
                }), Alignment.MIDDLE_RIGHT)
                .add(userTable, 1)
                .withSize(MSize.FULL_SIZE);
    }

    private MTable<MongoUserDetails> initUserTable() {
        MTable<MongoUserDetails> table = new MTable<>(MongoUserDetails.class)
                .withProperties("username", "role", "enabled")
                .withGeneratedColumn("worker",
                        user -> new MButton(VaadinIcons.USER,
                                user.getWorker() != null ? user.getWorker()
                                        .getFullName() : "not linked",
                                e -> {
                                    Notification.show("linking needs to get implemented");
                                }).withStyleName(ValoTheme.BUTTON_BORDERLESS))
                .withGeneratedColumn("changePw", user -> new MButton(VaadinIcons.KEY, e -> {
                    MPasswordField password = new MPasswordField("password")
                            .withRequired(true)
                            .withFullWidth();

                    MWindow window = new MWindow("change password")
                            .withModal(true)
                            .withDraggable(false)
                            .withResizable(false)
                            .withCenter();
                    window.setContent(new MVerticalLayout().add(password)
                            .add(new PrimaryButton("change", changeEvent -> {
                                mongoUserService.updatePassword(user, password.getValue());
                                Notification.show("successfully changed password");
                                window.close();
                            }))
                            .withWidth("300px"));
                    UI.getCurrent()
                            .addWindow(window);
                }).withStyleName(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_ICON_ONLY))
                .withGeneratedColumn("edit", user -> new MButton(VaadinIcons.PENCIL, e -> {
                    new MongoUserForm(user)
                            .initEditWindow((SavedHandler<MongoUserDetails>) entity -> {
                                mongoUserService.updateDetailsExceptPassword(entity);
                            }, (DeleteHandler<MongoUserDetails>) entity -> {
                                try {
                                    mongoUserService.delete(entity);
                                } catch (Exception exception) {
                                    Notification.show("you cannot delete yourself!", Type.ERROR_MESSAGE);
                                }
                            })
                            .addCloseListener(closeEvent -> {
                                refreshTab();
                            });
                }).withStyleName(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_ICON_ONLY))
                .withColumnHeaders("username", "role", "enabled", "linked worker", "password", "edit")
                .withColumnWidth("worker", 100)
                .withColumnWidth("password", 80)
                .withColumnWidth("edit", 80)
                .withColumnExpand("username", 1)
                .withSize(MSize.FULL_SIZE);
        return table;
    }


    @Override
    public void onTabEnter() {
        userTable.setBeans(mongoUserService.findAll());
    }
}
