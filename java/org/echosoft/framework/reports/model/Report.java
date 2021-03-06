package org.echosoft.framework.reports.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.echosoft.common.utils.StringUtil;
import org.echosoft.framework.reports.macros.Macros;
import org.echosoft.framework.reports.macros.MacrosRegistry;
import org.echosoft.framework.reports.model.el.Expression;
import org.echosoft.framework.reports.model.events.ReportEventListenerHolder;
import org.echosoft.framework.reports.model.providers.DataProviderHolder;

/**
 * <p>Описывает структуру одного отчета.</p>
 * <p>Каждый отчет может состоять из произвольного количества листов,
 * на каждом из которых может присутствовать произвольное количество разделов.
 * Каждый из таких разделов работает со своим источником данных и может представлять данные в виде
 * некоторых списочных форм различной сложности.</p>
 * 
 * @author Anton Sharapov
 */
public class Report implements Serializable {

    /**
     * Идентификатор отчета.
     */
    private final String id;

    /**
     * Краткое название отчета.
     */
    private String title;

    /**
     * Логин, который надо указать пользователю работающему с отчетом построенным на основе данного шаблона чтобы изменить данные
     * на тех листах отчета которые были помечены как защищенные (см. {@link Sheet#isProtected()}).
     * <strong>Внимание!</strong> защита от внесения изменений работает не на всех процессорах электронных таблиц!. 
     */
    private Expression user;

    /**
     * Пароль, который может указываться для защиты листов сгенерированного отчета от несанкционированного изменения.
     * <strong>Внимание!</strong> защита от внесения изменений работает не на всех процессорах электронных таблиц!.
     */
    private Expression password;

    /**
     * Шаблон отчета сериализованный в массив байт. Может быть <code>null</code>.
     * Если при построении экземпляра отчета это свойство не равно <code>null</code> то в выходной файл Excel будут
     * включаться все элементы файловой системы POI присутствующие в шаблоне отчета за исключением собственно секций отвечающих
     * за рабочую книгу (Workbook) документа и ее свойства.<br/>
     * Включение или не включение этой информации в итоговый отчет регулируется свойством <code>preserveTemplateData</code> в дескрипторе отчета.
     * По умолчанию эта информация в итоговый отчет не включается. 
     */
    private byte[] template;

    /**
     * Дополнительное описание отчета.
     */
    private final ReportDescription description;

    /**
     * Информация по всем листам отчета.
     */
    private final List<Sheet> sheets;

    /**
     * Перечень всех используемых в отчете стилей оформлений ячеек.
     */
    private final StylePalette palette;

    /**
     * Содержит функции определяемые пользователем которые были объявлены специально для данного отчета.
     */
    private final Map<String, Macros> macros;

    /**
     * Обработчики которые должны вызваться перед началом формирования экземпляра отчета.
     */
    private final List<ReportEventListenerHolder> listeners;

    /**
     * Перечень всех поставщиков данных, которые используются в отчете.
     */
    private final Map<String, DataProviderHolder> providers;

    public Report(String id, HSSFWorkbook wb) {
        id = StringUtil.trim(id);
        if (id==null)
            throw new IllegalArgumentException("Report identifier must be specified");
        this.id = id;
        this.description = new ReportDescription();
        this.sheets = new ArrayList<Sheet>();
        this.palette = new StylePalette(wb);
        this.macros = new HashMap<String,Macros>();
        this.listeners = new ArrayList<ReportEventListenerHolder>();
        this.providers = new HashMap<String,DataProviderHolder>();
    }

    /**
     * Полностью копирует модель отчета взяв за образец модель, переданную в аргументе.
     *
     * @param id   идентификатор отчета.
     * @param src  модель отчета используемая в качестве образца.
     * @throws CloneNotSupportedException  в случае проблем с клонированием какого-нибудь элемента листа.
     */
    public Report(String id, Report src) throws CloneNotSupportedException {
        id = StringUtil.trim(id);
        if (src==null)
            throw new IllegalArgumentException("All arguments must be specified");
        this.id = id!=null ? id : src.id;
        title = src.title;
        user = src.user;
        password = src.password;
        template = src.template;
        description = (ReportDescription)src.description.clone();
        palette = (StylePalette)src.palette.clone();
        macros = new HashMap<String,Macros>();
        listeners = new ArrayList<ReportEventListenerHolder>();
        providers = new HashMap<String,DataProviderHolder>();
        macros.putAll(src.macros);
        for (ReportEventListenerHolder listener : src.listeners) {
            listeners.add( (ReportEventListenerHolder)listener.clone() );
        }
        for (Map.Entry<String,DataProviderHolder> entry : src.providers.entrySet()) {
            providers.put(entry.getKey(), (DataProviderHolder)entry.getValue().clone());
        }
        sheets = new ArrayList<Sheet>();
        for (Sheet sheet : src.sheets) {
            sheets.add( sheet.cloneSheet(this) );
        }
    }

    /**
     * Возвращает идентификатор отчета.
     *
     * @return  идентификатор отчета.
     */
    public String getId() {
        return id;
    }

    /**
     * @return краткое название отчета.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает краткое название отчета.
     *
     * @param title  новое название отчета.
     */
    public void setTitle(String title) {
        this.title = title;
    }


    /**
     * Возвращает логин, который надо указать пользователю перед измением данных в тех листах сгенерированного отчета которые были помечены как "защищенные"
     * (см. {@link Sheet#isProtected()}).
     * <strong>Внимание!</strong> защита от внесения изменений работает не на всех процессорах электронных таблиц!.
     * @return выражение, вычисленный результат которого используется для определения имени пользователя, которому разрешена правка защищенных листов отчета.
     */
    public Expression getUser() {
        return user;
    }

    /**
     * Определяет логин, который надо указать пользователю перед измением данных в тех листах сгенерированного отчета которые были помечены как "защищенные"
     * (см. {@link Sheet#isProtected()}).
     * <strong>Внимание!</strong> защита от внесения изменений работает не на всех процессорах электронных таблиц!.
     * @param user  выражение, вычисленный результат которого используется для определения имени пользователя, которому разрешена правка защищенных листов отчета.
     */
    public void setUser(Expression user) {
        this.user = user;
    }

    /**
     * Возвращает пароль, который надо указать пользователю перед измением данных в тех листах сгенерированного отчета которые были помечены как "защищенные"
     * (см. {@link Sheet#isProtected()}).
     * <strong>Внимание!</strong> защита от внесения изменений работает не на всех процессорах электронных таблиц!.
     * @return выражение, вычисленный результат которого используется для определения пароля, к логину пользователя которому разрешена правка защищенных листов отчета.
     */
    public Expression getPassword() {
        return password;
    }

    /**
     * Устанавливает пароль которым должен быть защищен сгенерированный документ.
     *
     * @param password  строка пароля или <code>null</code>.
     */
    public void setPassword(Expression password) {
        this.password = password;
    }

    /**
     * Шаблон отчета сериализованный в массив байт. Может быть <code>null</code>.
     * Если при построении экземпляра отчета это свойство не равно <code>null</code> то в выходной файл Excel будут
     * включаться все элементы файловой системы POI присутствующие в шаблоне отчета за исключением собственно секций отвечающих
     * за рабочую книгу (Workbook) документа и ее свойства.<br/>
     * Включение или не включение этой информации в итоговый отчет регулируется свойством <code>preserveTemplateData</code> в дескрипторе отчета.
     * По умолчанию эта информация в итоговый отчет не включается.
     * @return Дополнительные секции данных из шаблона отчета которые должны включаться в каждый генерируемый экземпляр отчета.
     */
    public byte[] getTemplate() {
        return template;
    }
    public void setTemplate(byte[] template) {
        this.template = template;
    }

    /**
     * Возвращает дополнительную информацию, которая при построении отчета будет транслирована в соответствующие
     * свойства документа excel.
     *
     * @return  дополнительное описание отчета.
     */
    public ReportDescription getDescription() {
        return description;
    }

    /**
     * Возвращает информацию по всем стилям оформления ячеек используемым в отчете.
     *
     * @return информация по всем используемым стилям ячеек.
     */
    public StylePalette getPalette() {
        return palette;
    }


    /**
     * Осуществляет поиск секции по всему отчету.
     *
     * @param sectionId  идентификатор секции.
     * @return  информация об указанной секции или null если секция с таким идентификатором отсутствует в отчете.
     */
    public Section findSectionById(String sectionId) {
        Section result;
        for (Sheet sheet : sheets) {
            result = sheet.findSectionById(sectionId);
            if (result!=null)
                return result;
        }
        return null;
    }

    /**
     * Осуществляет поиск листа отчета по его идентификатору.
     *
     * @param sheetId  идентификатор отчета. Не может быть пустой строкой или null.
     * @return  информация об искомом листе отчета или <code>null</code> если лист с таким идентификатором отсутствует в отчете.
     */
    public Sheet findSheetById(final String sheetId) {
        for (Sheet sheet : sheets) {
            if (sheet.getId().equals(sheetId))
                return sheet;
        }
        return null;
    }

    /**
     * Возвращает список всех листов в отчете строго в той последовательности в которой они
     * должны быть представлены в итоговом отчете.
     *
     * @return  список листов отчета.
     */
    public List<Sheet> getSheets() {
        return sheets;
    }

    /**
     * Возвращает макросы зарегистрированные локально только для данного отчета. Они могут также переопределять глобальные
     * макросы зарегистрированные под тем же именем.
     *
     * @return все локальные макро-функции зарегистрированные для данного вида отчетов.
     */
    public Map<String,Macros> getLocalMacros() {
        return macros;
    }

    /**
     * Возвращает макро-функцию по ее имени. Сначала поиск осуществляется среди функций зарегистрированных локально
     * только для данного отчета. Если таковая функция не была найдена то поиск осуществляется в глобальном
     * {@link MacrosRegistry реестре} макрофункций.
     *
     * @param name  имя функции (чуствительно к регистру).
     * @return  Соответствующая функция или <code>null</code>.
     */
    public Macros getMacros(String name) {
        Macros result = macros.get(name);
        if (result==null) {
            result = MacrosRegistry.getMacros(name);
        }
        return result;
    }

    /**
     * Возвращает список всех обработчиков которые вызываются при начале формирования нового экземпляра отчета.
     * Если в отчете не зарегистрировано ни одного обработчика события то метод возвращает пустой список.
     *
     * @return список всех зарегистрированных обработчиков события "формирование нового экземпляра отчета".
     */
    public List<ReportEventListenerHolder> getListeners() {
        return listeners;
    }

    /**
     * Перечень всех поставщиков данных, которые используются в отчете.
     *
     * @return  все поставщики данных используемые в отчете.
     */
    public Map<String,DataProviderHolder> getProviders() {
        return providers;
    }



    @Override
    public String toString() {
        return "[Report{id:"+id+", title:"+title+"}]";
    }
}
