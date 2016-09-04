/**
 * Copyright (c) 2013-2016, Jieven. All rights reserved.
 * <p/>
 * Licensed under the GPL license: http://www.gnu.org/licenses/gpl.txt
 * To use it on other terms please contact us at 1623736450@qq.com
 */
package com.eova.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.beetl.core.GroupTemplate;
import org.beetl.ext.jfinal.BeetlRenderFactory;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.wall.WallFilter;
import com.eova.common.plugin.quartz.QuartzPlugin;
import com.eova.common.utils.xx;
import com.eova.core.IndexController;
import com.eova.core.auth.AuthController;
import com.eova.core.button.ButtonController;
import com.eova.core.menu.MenuController;
import com.eova.core.meta.MetaController;
import com.eova.core.task.TaskController;
import com.eova.interceptor.LoginInterceptor;
import com.eova.model.Button;
import com.eova.model.EovaLog;
import com.eova.model.Menu;
import com.eova.model.MenuObject;
import com.eova.model.MetaField;
import com.eova.model.MetaObject;
import com.eova.model.Role;
import com.eova.model.RoleBtn;
import com.eova.model.Task;
import com.eova.model.User;
import com.eova.model.Widget;
import com.eova.service.ServiceManager;
import com.eova.template.common.config.TemplateConfig;
import com.eova.template.masterslave.MasterSlaveController;
import com.eova.template.single.SingleController;
import com.eova.template.singletree.SingleTreeController;
import com.eova.template.treetogrid.TreeToGridController;
import com.eova.widget.WidgetController;
import com.eova.widget.form.FormController;
import com.eova.widget.grid.GridController;
import com.eova.widget.tree.TreeController;
import com.eova.widget.treegrid.TreeGridController;
import com.eova.widget.upload.UploadController;
import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.CaseInsensitiveContainerFactory;
import com.jfinal.plugin.activerecord.IDataSourceProvider;
import com.jfinal.plugin.activerecord.dialect.AnsiSqlDialect;
import com.jfinal.plugin.activerecord.dialect.Dialect;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.activerecord.dialect.OracleDialect;
import com.jfinal.plugin.activerecord.dialect.PostgreSqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.druid.DruidStatViewHandler;
import com.jfinal.plugin.ehcache.EhCachePlugin;

public class EovaConfig extends JFinalConfig {

	/** Eova配置属性 **/
	public static Map<String, String> props = new HashMap<String, String>();
	/** EOVA所在数据库的类型 **/
	public static String EOVA_DBTYPE = "mysql";
	/** 数据源列表 **/
	public static List<String> dataSources = new ArrayList<String>();
	
	private long startTime = 0;

	/**
	 * 系统启动之后
	 */
	@Override
	public void afterJFinalStart() {
		System.err.println("JFinal Started\n");
		// Load Cost Time
		costTime(startTime);

		{
			Boolean isInit = xx.toBoolean(props.get("initPlugins"), true);
			if (isInit) {
				EovaInit.initPlugins();
			}
		}
		{
			Boolean isInit = xx.toBoolean(props.get("initSql"), false);
			if (isInit && EOVA_DBTYPE.equals(JdbcUtils.MYSQL)) {
				EovaInit.initCreateSql();
			}
		}
	}

	/**
	 * 系统停止之前
	 */
	@Override
	public void beforeJFinalStop() {
	}

	/**
	 * 配置常量
	 */
	@Override
	public void configConstant(Constants me) {
		startTime = System.currentTimeMillis();

		System.err.println("Config Constants Starting...");
		// 初始化配置
		EovaInit.initConfig();
		// 开发模式
		me.setDevMode(xx.toBoolean(props.get("devMode"), true));
		// 设置主视图为Beetl
		me.setMainRenderFactory(new BeetlRenderFactory());
		// POST内容最大500M(安装包上传)
		me.setMaxPostSize(1024 * 1024 * 500);
		me.setError500View("/eova/500.html");
		me.setError404View("/eova/404.html");
		
		// 设置静态根目录为上传根目录
		me.setBaseUploadPath(props.get("static_root"));

		@SuppressWarnings("unused")
		GroupTemplate group = BeetlRenderFactory.groupTemplate;

		// 设置全局变量
		final String STATIC = props.get("domain_static");
		final String CDN = props.get("domain_cdn");
		final String IMG = props.get("domain_img");
		final String FILE = props.get("domain_file");
		
		Map<String, Object> sharedVars = new HashMap<String, Object>();
		if (!xx.isEmpty(STATIC))
			sharedVars.put("STATIC", STATIC);
		else
			sharedVars.put("STATIC", "");
		if (!xx.isEmpty(CDN))
			sharedVars.put("CDN", CDN);
		if (!xx.isEmpty(IMG))
			sharedVars.put("IMG", IMG);
		if (!xx.isEmpty(FILE))
			sharedVars.put("FILE", FILE);
		
		// Load Template Const
		 PageConst.init(sharedVars);

		BeetlRenderFactory.groupTemplate.setSharedVars(sharedVars);
	}

	/**
	 * 配置路由
	 */
	@Override
	public void configRoute(Routes me) {
		System.err.println("Config Routes Starting...");
		me.add("/", IndexController.class);
		// 业务模版
		me.add(TemplateConfig.SINGLE_GRID, SingleController.class);
		me.add(TemplateConfig.SINGLE_TREE, SingleTreeController.class);
		me.add(TemplateConfig.MASTER_SLAVE_GRID, MasterSlaveController.class);
		me.add(TemplateConfig.TREE_GRID, TreeToGridController.class);
		// 组件
		me.add("/widget", WidgetController.class);
		me.add("/upload", UploadController.class);
		me.add("/form", FormController.class);
		me.add("/grid", GridController.class);
		me.add("/tree", TreeController.class);
		me.add("/treegrid", TreeGridController.class);
		// Eova业务
		me.add("/meta", MetaController.class);
		me.add("/menu", MenuController.class);
		me.add("/button", ButtonController.class);
		me.add("/auth", AuthController.class);
		me.add("/task", TaskController.class);

		route(me);

	}

	/**
	 * 配置插件
	 */
	@Override
	public void configPlugin(Plugins plugins) {
		System.err.println("Config Plugins Starting...");

		String eovaUrl, eovaUser, eovaPwd;
		String mainUrl, mainUser, mainPwd;

		eovaUrl = props.get("eova_url");
		eovaUser = props.get("eova_user");
		eovaPwd = props.get("eova_pwd");

		mainUrl = props.get("main_url");
		mainUser = props.get("main_user");
		mainPwd = props.get("main_pwd");

		// 默认数据源
		{
			DruidPlugin dp = initDruidPlugin(mainUrl, mainUser, mainPwd);
			ActiveRecordPlugin arp = initActiveRecordPlugin(mainUrl, xx.DS_MAIN, dp);
			System.out.println("load data source:" + mainUrl + "/" + mainUser);

			mapping(arp);
			
			plugins.add(dp).add(arp);
		}
		
		// Eova数据源
		{
			DruidPlugin dp = initDruidPlugin(eovaUrl, eovaUser, eovaPwd);
			ActiveRecordPlugin arp = initActiveRecordPlugin(eovaUrl, xx.DS_EOVA, dp);
			System.out.println("load eova datasource:" + eovaUrl + "/" + eovaUser);

			mappingEova(arp);

			plugins.add(dp).add(arp);

			try {
				// Eova的数据库类型
				EOVA_DBTYPE = JdbcUtils.getDbType(eovaUrl, JdbcUtils.getDriverClassName(eovaUrl));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		//  自定义插件
		plugin(plugins);
		
		// 初始化ServiceManager
		ServiceManager.init();

		// 配置EhCachePlugin插件
		plugins.add(new EhCachePlugin());

		// 配置Quartz
		QuartzPlugin quartz = new QuartzPlugin();
		plugins.add(quartz);
	}

	/**
	 * 配置全局拦截器
	 */
	@Override
	public void configInterceptor(Interceptors me) {
		System.err.println("Config Interceptors Starting...");
		// JFinal.me().getServletContext().setAttribute("KING", "我笑了");
		// 登录验证
		me.add(new LoginInterceptor());
		// 权限验证拦截(暂时屏蔽权限拦截，待后续完善)
		// me.add(new AuthInterceptor());
	}

	/**
	 * 配置处理器
	 */
	@Override
	public void configHandler(Handlers me) {
		System.err.println("Config Handlers Starting...");
		// 添加DruidHandler
		DruidStatViewHandler dvh = new DruidStatViewHandler("/druid");
		me.add(dvh);
	}

	/**
	 * Eova Data Source Model Mapping
	 * 
	 * @param arp
	 */
	protected void mappingEova(ActiveRecordPlugin arp) {
		arp.addMapping("eova_object", MetaObject.class);
		arp.addMapping("eova_field", MetaField.class);
		arp.addMapping("eova_button", Button.class);
		arp.addMapping("eova_menu", Menu.class);
		arp.addMapping("eova_menu_object", MenuObject.class);
		arp.addMapping("eova_user", User.class);
		arp.addMapping("eova_role", Role.class);
		arp.addMapping("eova_role_btn", RoleBtn.class);
		arp.addMapping("eova_log", EovaLog.class);
		arp.addMapping("eova_task", Task.class);
		arp.addMapping("eova_widget", Widget.class);
	}

	/**
	 * Main Data Source Model Mapping
	 * 
	 * @param arp
	 */
	protected void mapping(ActiveRecordPlugin arp) {
	}

	/**
	 * Custom Route
	 * 
	 * @param me
	 */
	protected void route(Routes me) {
	}
	
	/**
	 * Custom Plugin
	 * @param plugins
	 * @return
	 */
	protected void plugin(Plugins plugins) {
	}

	/**
	 * init Druid
	 * 
	 * @param url JDBC
	 * @param username 数据库用户
	 * @param password 数据库密码
	 * @return
	 */
	protected DruidPlugin initDruidPlugin(String url, String username, String password) {
		// 设置方言
		WallFilter wall = new WallFilter();
		String dbType = null;
		try {
			dbType = JdbcUtils.getDbType(url, JdbcUtils.getDriverClassName(url));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		wall.setDbType(dbType);

		DruidPlugin dp = new DruidPlugin(url, username, password);
		dp.addFilter(new StatFilter());
		dp.addFilter(wall);
		return dp;

	}

	/**
	 * init ActiveRecord
	 * 
	 * @param url JDBC
	 * @param ds DataSource
	 * @param dp Druid
	 * @return
	 */
	protected ActiveRecordPlugin initActiveRecordPlugin(String url, String ds, IDataSourceProvider dp) {
		ActiveRecordPlugin arp = new ActiveRecordPlugin(ds, dp);
		// 提升事务级别保证事务回滚
		int lv = xx.toInt(props.get("transaction_level"), Connection.TRANSACTION_REPEATABLE_READ);
		arp.setTransactionLevel(lv);

		String dbType;
		try {
			dbType = JdbcUtils.getDbType(url, JdbcUtils.getDriverClassName(url));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		// 统一全部默认小写
		arp.setContainerFactory(new CaseInsensitiveContainerFactory(true));

		Dialect dialect;
		if (JdbcUtils.MYSQL.equalsIgnoreCase(dbType) || JdbcUtils.H2.equalsIgnoreCase(dbType)) {
			dialect = new MysqlDialect();
		} else if (JdbcUtils.ORACLE.equalsIgnoreCase(dbType)) {
			dialect = new OracleDialect();
			((DruidPlugin) dp).setValidationQuery("select 1 FROM DUAL");
		} else if (JdbcUtils.POSTGRESQL.equalsIgnoreCase(dbType)) {
			dialect = new PostgreSqlDialect();
		} else {
			// 默认使用标准SQL方言
			dialect = new AnsiSqlDialect();
		}
		arp.setDialect(dialect);

		// 是否显示SQL
		Boolean isShow = xx.toBoolean(props.get("showSql"), false);
		if (isShow != null) {
			arp.setShowSql(isShow);
		}

		// 记录数据源
		dataSources.add(ds);

		return arp;
	}

	private void costTime(long time) {
		System.err.println("Load Cost Time:" + (System.currentTimeMillis() - time) + "ms\n");
	}

}