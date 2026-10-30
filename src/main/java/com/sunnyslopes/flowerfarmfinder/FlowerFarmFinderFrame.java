package com.sunnyslopes.flowerfarmfinder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.MessageFormat;

import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.DEFAULT_MC_VERSION;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_LONG_DEFAULT;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_LONG_MAX;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_LONG_MIN;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_SHORT_DEFAULT;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_SIDE_LEGACY;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_VIEW_DEFAULT;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.LIST_RANGE_SIDE_LATEST;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.LIST_RANGE_SIDE_LEGACY;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.SEARCH_RANGE_SIDE_LATEST;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.SEARCH_RANGE_SIDE_LEGACY;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FILTER_ORIENTATION_ANY;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FILTER_ORIENTATION_HORIZONTAL;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FILTER_ORIENTATION_VERTICAL;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.FLOWER_CENTER_MARGIN_DEFAULT;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.PHASE_MERGE;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.PHASE_PREPARING;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.PHASE_VERIFY;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.SUPPORTED_MC_VERSIONS;
import static com.sunnyslopes.flowerfarmfinder.FlowerFarmFinderSearchRunner.isLatestMcRange;

public class FlowerFarmFinderFrame extends JFrame {
    // ResourceBundle for internationalization
    private ResourceBundle messages;
    // World border (block coords); side = 60000001
    private static final int BOUNDARY_MIN_X = -30000000;
    private static final int BOUNDARY_MAX_X = 30000000;
    private static final int BOUNDARY_MIN_Z = -30000000;
    private static final int BOUNDARY_MAX_Z = 30000000;

    // 单种子搜索相关组件
    private JLabel searchSeedLabel;
    private JLabel searchThreadCountLabel;
    private JLabel searchLongSideLabel;
    private JLabel searchShortSideLabel;
    private JLabel searchViewDistanceLabel;
    private JCheckBox searchFastModeCheckBox;
    private JCheckBox searchBothWildflowersCheckBox;
    private JCheckBox searchBasaltCheckBox;
    private JCheckBox searchOrientationCheckBox;
    private JComboBox<String> searchOrientationComboBox;
    private JCheckBox searchFlowerCenterCheckBox;
    private JTextField searchFlowerCenterField;
    private JLabel searchMcVersionLabel;
    private JComboBox<String> searchMcVersionComboBox;
    private JLabel searchMinXLabel;
    private JLabel searchMaxXLabel;
    private JLabel searchMinZLabel;
    private JLabel searchMaxZLabel;
    private JLabel searchLanguageLabel;
    private JTextField searchSeedField;
    private JTextField searchThreadCountField;
    private JTextField searchLongSideField;
    private JCheckBox searchShortSideCheckBox;
    private JTextField searchShortSideField;
    private JTextField searchViewDistanceField;
    private boolean searchFarmUpdating = false;
    private JTextField minXField;
    private JTextField maxXField;
    private JTextField minZField;
    private JTextField maxZField;
    private JCheckBox squareSideCheckBox;
    private JTextField squareSideField;
    private boolean squareSideUpdating = false;
    private JComboBox<String> languageComboBox;
    private JButton searchStartButton;
    private JButton searchPauseButton;
    private JButton searchStopButton;
    private JButton searchResetButton;
    private JButton searchResetToWorldBorderButton;
    private JButton searchExportButton;
    private JButton searchSortButton;
    private JProgressBar searchProgressBar;
    private JLabel searchElapsedTimeLabel;
    private JLabel searchRemainingTimeLabel;
    private JLabel searchCreditLabel;
    private JPanel searchRightPanel;
    private JTextArea searchResultArea;
    private JLabel listSearchCreditLabel;
    private JPanel listSearchRightPanel;
    private FlowerFarmFinderSearchRunner searchRunner;
    private volatile boolean isSearchRunning = false;
    private volatile boolean isSearchPaused = false;
    private long lastSearchSeed = 0;
    private int lastSearchMinX = 0;
    private int lastSearchMaxX = 0;
    private int lastSearchMinZ = 0;
    private int lastSearchMaxZ = 0;
    private int lastSearchMinArea = 0;
    private int lastSearchThreadCount = 0;
    /**
     * Monotonic token: increment before clearing single-seed results, again on stop/done so any
     * invokeLater from a previous run sees token mismatch and does not append (fixes stale lines after new search).
     */
    private final AtomicLong singleSearchResultToken = new AtomicLong(0);
    private final AtomicLong listSearchResultToken = new AtomicLong(0);

    // 从种子列表搜索相关组件
    private JButton listSearchSeedFileButton;
    private JLabel listSearchSeedFileLabel;
    private JLabel listSearchSeedFileTitleLabel;
    private JLabel listSearchThreadCountLabel;
    private JLabel listLongSideLabel;
    private JLabel listShortSideLabel;
    private JLabel listViewDistanceLabel;
    private JCheckBox listFastModeCheckBox;
    private JCheckBox listBothWildflowersCheckBox;
    private JCheckBox listBasaltCheckBox;
    private JCheckBox listOrientationCheckBox;
    private JComboBox<String> listOrientationComboBox;
    private JCheckBox listFlowerCenterCheckBox;
    private JTextField listFlowerCenterField;
    private JLabel listMcVersionLabel;
    private JComboBox<String> listMcVersionComboBox;
    private JLabel listSearchMinXLabel;
    private JLabel listSearchMaxXLabel;
    private JLabel listSearchMinZLabel;
    private JLabel listSearchMaxZLabel;
    private File selectedSeedFile;
    private JTextField listSearchThreadCountField;
    private JTextField listLongSideField;
    private JCheckBox listShortSideCheckBox;
    private JTextField listShortSideField;
    private JTextField listViewDistanceField;
    private boolean listFarmUpdating = false;
    private JTextField listMinXField;
    private JTextField listMaxXField;
    private JTextField listMinZField;
    private JTextField listMaxZField;
    private JCheckBox listSquareSideCheckBox;
    private JTextField listSquareSideField;
    private boolean listSquareSideUpdating = false;
    private JButton listSearchStartButton;
    private JButton listSearchPauseButton;
    private JButton listSearchStopButton;
    private JButton listSearchResetButton;
    private JButton listSearchExportButton;
    private JButton listSearchExportSeedListButton;
    private JButton listSortButton;
    private JProgressBar listSearchProgressBar;
    private JLabel listSearchElapsedTimeLabel;
    private JLabel listSearchRemainingTimeLabel;
    private JLabel listSearchCurrentSeedProgressLabel;
    private JTextArea listSearchResultArea;
    private FlowerFarmFinderSearchRunner listSearchRunner;
    private volatile boolean isListSearchRunning = false;
    private volatile boolean isListSearchPaused = false;
    private int lastListSearchMinX = 0;
    private int lastListSearchMaxX = 0;
    private int lastListSearchMinZ = 0;
    private int lastListSearchMaxZ = 0;
    private int lastListSearchMinArea = 0;
    private int lastListSearchThreadCount = 0;
    // 存储每个种子的结果
    private final Map<Long, List<String>> seedResults = new HashMap<>();


    // 加载的字体
    private Font loadedFont = null;
    // 当前语言Locale
    private Locale currentLocale;

    public FlowerFarmFinderFrame() {
        // 初始化ResourceBundle，根据系统语言选择
        Locale systemLocale = Locale.getDefault();
        // 如果系统语言是中文（zh-cn、zh-hk、zh-tw），使用中文资源，否则使用英文
        if (systemLocale.getLanguage().equals("zh")) {
            String country = systemLocale.getCountry().toLowerCase();
            if (country.equals("cn") || country.equals("hk") || country.equals("tw")) {
                currentLocale = new Locale("zh", "CN");
            } else {
                currentLocale = new Locale("en", "US");
            }
        } else {
            currentLocale = new Locale("en", "US");
        }
        try {
            messages = ResourceBundle.getBundle("messages", currentLocale);
        } catch (Exception e) {
            // 如果加载失败，使用默认的英文
            currentLocale = new Locale("en", "US");
            messages = ResourceBundle.getBundle("messages", currentLocale);
        }

        setTitle(getString("window.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 设置窗口图标
        setWindowIcon();

        // 设置中文字体
        setChineseFont();

        // 创建标签页
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(getString("tab.singleSeedSearch"), createSingleSeedSearchPanel());
        tabbedPane.addTab(getString("tab.listSearch"), createListSearchPanel());
        add(tabbedPane, BorderLayout.CENTER);

        pack();
        setSize(1500, 1000);
        setLocationRelativeTo(null);
    }

    /** Credit area hints (hard/soft requirements, fast mode, etc.). */
    private String buildCreditMemoryPhaseHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString("hint.hardReq")).append("<br>");
        sb.append(getString("hint.softReq")).append("<br>");
        sb.append(getString("hint.samplingY")).append("<br>");
        sb.append(getString("hint.fastModeNote")).append("<br>");
        sb.append(getString("hint.filters")).append("<br>");
        sb.append(getString("hint.outputHint"));
        return sb.toString();
    }

    /** Square side to inclusive min (centered on origin). */
    private static int rangeMinForSide(int side) {
        return -side / 2;
    }

    /** Square side to inclusive max (centered on origin). */
    private static int rangeMaxForSide(int side) {
        return side / 2 - 1;
    }

    private int defaultSearchRangeSide() {
        return isLatestMcRange(selectedMcVersion(searchMcVersionComboBox))
                ? SEARCH_RANGE_SIDE_LATEST : SEARCH_RANGE_SIDE_LEGACY;
    }

    private int defaultListRangeSide() {
        return isLatestMcRange(selectedMcVersion(listMcVersionComboBox))
                ? LIST_RANGE_SIDE_LATEST : LIST_RANGE_SIDE_LEGACY;
    }

    private void applyDefaultSearchRange() {
        int side = defaultSearchRangeSide();
        applySearchBounds(rangeMinForSide(side), rangeMaxForSide(side),
                rangeMinForSide(side), rangeMaxForSide(side));
    }

    private void applyDefaultListRange() {
        int side = defaultListRangeSide();
        applyListSearchBounds(rangeMinForSide(side), rangeMaxForSide(side),
                rangeMinForSide(side), rangeMaxForSide(side));
    }

    private boolean isSearchFastMode() {
        return searchFastModeCheckBox == null || searchFastModeCheckBox.isSelected();
    }

    private boolean isListFastMode() {
        return listFastModeCheckBox == null || listFastModeCheckBox.isSelected();
    }

    /** Optional filters: both wildflower bonuses in the same window. */
    private boolean isBothWildflowersFilter(JCheckBox box) {
        return box != null && box.isSelected();
    }

    /** Optional filters: nether window contains basalt deltas. */
    private boolean isBasaltFilter(JCheckBox box) {
        return box != null && box.isSelected();
    }

    /** Orientation filter: 0 any, 1 horizontal (X span >= Z span), 2 vertical (Z span >= X span). */
    private int selectedOrientation(JCheckBox box, JComboBox<String> combo) {
        if (box == null || !box.isSelected() || combo == null) {
            return FILTER_ORIENTATION_ANY;
        }
        return combo.getSelectedIndex() == 1 ? FILTER_ORIENTATION_VERTICAL : FILTER_ORIENTATION_HORIZONTAL;
    }

    /** The axis dropdown is only editable while its checkbox is on and no search runs. */
    private void applyOrientationEnable(boolean searchTab) {
        JCheckBox box = searchTab ? searchOrientationCheckBox : listOrientationCheckBox;
        JComboBox<String> combo = searchTab ? searchOrientationComboBox : listOrientationComboBox;
        boolean running = searchTab ? isSearchRunning : isListSearchRunning;
        if (combo != null) {
            combo.setEnabled(!running && box != null && box.isSelected());
        }
    }

    /**
     * Enable/disable the optional filters. "Both wildflowers" only makes sense on
     * 1.21.4+ (1.18~1.21.3 has no wildflowers bonus), so it is disabled there.
     */
    private void applyFilterEnable(boolean searchTab) {
        boolean running = searchTab ? isSearchRunning : isListSearchRunning;
        JComboBox<String> versionCombo = searchTab ? searchMcVersionComboBox : listMcVersionComboBox;
        boolean latest = isLatestMcRange(selectedMcVersion(versionCombo));
        JCheckBox both = searchTab ? searchBothWildflowersCheckBox : listBothWildflowersCheckBox;
        JCheckBox basalt = searchTab ? searchBasaltCheckBox : listBasaltCheckBox;
        JCheckBox orientation = searchTab ? searchOrientationCheckBox : listOrientationCheckBox;
        if (both != null) {
            if (!latest) {
                both.setSelected(false);
            }
            both.setEnabled(!running && latest);
        }
        if (basalt != null) {
            basalt.setEnabled(!running);
        }
        if (orientation != null) {
            orientation.setEnabled(!running);
        }
        applyOrientationEnable(searchTab);
        applyFlowerCenterEnable(searchTab);
    }

    /**
     * Flower-forest edge margin: the checkbox is editable whenever no search runs,
     * the distance field only while the filter is checked.
     */
    private void applyFlowerCenterEnable(boolean searchTab) {
        JCheckBox box = searchTab ? searchFlowerCenterCheckBox : listFlowerCenterCheckBox;
        JTextField field = searchTab ? searchFlowerCenterField : listFlowerCenterField;
        boolean running = searchTab ? isSearchRunning : isListSearchRunning;
        if (box != null) {
            box.setEnabled(!running);
        }
        if (field != null) {
            field.setEnabled(!running && box != null && box.isSelected());
        }
    }

    /**
     * Reads the flower-forest edge margin.
     *
     * @return 0 when the filter is off, the distance in blocks when valid,
     *     -1 when the value is not a non-negative integer,
     *     -2 when the distance leaves no centre area inside the demand window.
     */
    private int readFlowerCenterMargin(JCheckBox box, JTextField field, int sideX, int sideZ) {
        if (box == null || !box.isSelected()) {
            return FLOWER_CENTER_MARGIN_DEFAULT;
        }
        if (field == null) {
            return FLOWER_CENTER_MARGIN_DEFAULT;
        }
        int margin;
        try {
            margin = Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
        if (margin < 0) {
            return -1;
        }
        if (margin > (Math.min(sideX, sideZ) - 1) / 2) {
            return -2;
        }
        return margin;
    }

    /** Reports an invalid flower-forest edge margin; returns false for "no error". */
    private boolean reportFlowerCenterMarginError(int margin, int sideX, int sideZ) {
        if (margin == -1) {
            JOptionPane.showMessageDialog(this, getString("error.flowerCenterInvalid"),
                    getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            return true;
        }
        if (margin == -2) {
            int max = (Math.min(sideX, sideZ) - 1) / 2;
            JOptionPane.showMessageDialog(this, getString("error.flowerCenterTooLarge", max),
                    getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            return true;
        }
        return false;
    }

    /** Re-localizes the horizontal/vertical dropdown without losing the selection. */
    private void refreshOrientationCombo(JComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        int selected = Math.max(0, Math.min(1, combo.getSelectedIndex()));
        combo.setModel(new DefaultComboBoxModel<>(new String[]{
                getString("label.orientationHorizontal"), getString("label.orientationVertical")}));
        combo.setSelectedIndex(selected);
    }

    private String selectedMcVersion(JComboBox<String> combo) {
        if (combo == null || combo.getSelectedItem() == null) {
            return DEFAULT_MC_VERSION;
        }
        return combo.getSelectedItem().toString();
    }

    private boolean mcVersionSideUpdating;

    /** On version change: legacy farm window 320×320; also refresh default search-range side. */
    private void onMcVersionChanged(boolean searchTab) {
        if (mcVersionSideUpdating) {
            return;
        }
        mcVersionSideUpdating = true;
        try {
            JComboBox<String> combo = searchTab ? searchMcVersionComboBox : listMcVersionComboBox;
            JTextField longField = searchTab ? searchLongSideField : listLongSideField;
            JTextField shortField = searchTab ? searchShortSideField : listShortSideField;
            JCheckBox shortBox = searchTab ? searchShortSideCheckBox : listShortSideCheckBox;
            boolean running = searchTab ? isSearchRunning : isListSearchRunning;
            if (!isLatestMcRange(selectedMcVersion(combo))) {
                if (longField != null) {
                    longField.setText(String.valueOf(FLOWER_SIDE_LEGACY));
                    rememberSide(longField, FLOWER_SIDE_LEGACY);
                }
                if (shortField != null) {
                    shortField.setText(String.valueOf(FLOWER_SIDE_LEGACY));
                    rememberSide(shortField, FLOWER_SIDE_LEGACY);
                }
                if (shortBox != null) {
                    shortBox.setSelected(true);
                }
            }
            if (!running) {
                if (searchTab) {
                    applyDefaultSearchRange();
                } else {
                    applyDefaultListRange();
                }
            }
            boolean editable = !running;
            if (longField != null) longField.setEnabled(editable);
            if (shortBox != null) shortBox.setEnabled(editable);
            if (shortField != null) {
                shortField.setEnabled(editable && (shortBox == null || shortBox.isSelected()));
            }
            JTextField viewField = searchTab ? searchViewDistanceField : listViewDistanceField;
            if (viewField != null) viewField.setEnabled(editable);
            applyFilterEnable(searchTab);
        } finally {
            mcVersionSideUpdating = false;
        }
    }

    private void applyShortSideEnable(boolean searchTab) {
        JCheckBox shortBox = searchTab ? searchShortSideCheckBox : listShortSideCheckBox;
        JTextField shortField = searchTab ? searchShortSideField : listShortSideField;
        boolean running = searchTab ? isSearchRunning : isListSearchRunning;
        if (shortField != null) {
            shortField.setEnabled(!running && (shortBox == null || shortBox.isSelected()));
        }
    }

    private void enableSideFields(boolean searchTab) {
        boolean running = searchTab ? isSearchRunning : isListSearchRunning;
        JTextField longField = searchTab ? searchLongSideField : listLongSideField;
        JCheckBox shortBox = searchTab ? searchShortSideCheckBox : listShortSideCheckBox;
        JTextField shortField = searchTab ? searchShortSideField : listShortSideField;
        JTextField viewField = searchTab ? searchViewDistanceField : listViewDistanceField;
        if (longField != null) longField.setEnabled(!running);
        if (shortBox != null) shortBox.setEnabled(!running);
        if (shortField != null) {
            shortField.setEnabled(!running && (shortBox == null || shortBox.isSelected()));
        }
        if (viewField != null) viewField.setEnabled(!running);
    }

    private int parseSideField(JTextField field, int min, int max) {
        if (field == null) return -1;
        try {
            int v = Integer.parseInt(field.getText().trim());
            return (v >= min && v <= max) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int acceptedSide(JTextField field, int fallback) {
        if (field == null) return fallback;
        Object stored = field.getClientProperty("acceptedSide");
        return stored instanceof Integer ? (Integer) stored : fallback;
    }

    private void rememberSide(JTextField field, int value) {
        if (field != null) {
            field.putClientProperty("acceptedSide", value);
        }
    }

    private void commitLongSide(JTextField longField, JTextField shortField) {
        int previous = acceptedSide(longField, FLOWER_LONG_DEFAULT);
        int longSide = parseSideField(longField, FLOWER_LONG_MIN, FLOWER_LONG_MAX);
        if (longSide < 0) {
            JOptionPane.showMessageDialog(this, getString("error.longSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            longField.setText(String.valueOf(previous));
            return;
        }
        int shortSide = parseSideField(shortField, 1, FLOWER_LONG_MAX);
        if (shortSide > longSide) {
            JOptionPane.showMessageDialog(this, getString("error.shortSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            longField.setText(String.valueOf(previous));
            return;
        }
        rememberSide(longField, longSide);
    }

    private void commitShortSide(JTextField longField, JTextField shortField) {
        int previous = acceptedSide(shortField, FLOWER_SHORT_DEFAULT);
        int longSide = parseSideField(longField, FLOWER_LONG_MIN, FLOWER_LONG_MAX);
        if (longSide < 0) longSide = FLOWER_LONG_DEFAULT;
        int shortSide = parseSideField(shortField, 1, FLOWER_LONG_MAX);
        if (shortSide < 0 || shortSide > longSide) {
            JOptionPane.showMessageDialog(this, getString("error.shortSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            shortField.setText(String.valueOf(previous));
            return;
        }
        rememberSide(shortField, shortSide);
    }

    private void applyViewDistance(JTextField viewField, JTextField longField, JCheckBox shortBox) {
        int view = parseSideField(viewField, 8, 32);
        if (view < 0) {
            JOptionPane.showMessageDialog(this, getString("error.viewDistanceInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            viewField.setText(String.valueOf(FLOWER_VIEW_DEFAULT));
            return;
        }
        // Square demand area: 32 blocks per simulation-distance chunk + 16
        // blocks margin.  The long-side cap is 1024, so view=32 is clamped.
        int side = view * 32 + 16;
        if (side > FLOWER_LONG_MAX) {
            side = FLOWER_LONG_MAX;
        }
        if (side < FLOWER_LONG_MIN) {
            JOptionPane.showMessageDialog(this, getString("error.viewDistanceInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        longField.setText(String.valueOf(side));
        rememberSide(longField, side);
        if (shortBox != null) {
            shortBox.setSelected(false);
            if (shortBox == searchShortSideCheckBox && searchShortSideField != null) {
                searchShortSideField.setText(String.valueOf(side));
                rememberSide(searchShortSideField, side);
                searchShortSideField.setEnabled(false);
            }
            if (shortBox == listShortSideCheckBox && listShortSideField != null) {
                listShortSideField.setText(String.valueOf(side));
                rememberSide(listShortSideField, side);
                listShortSideField.setEnabled(false);
            }
        }
    }

    private int readLongSide(JTextField field) {
        return parseSideField(field, FLOWER_LONG_MIN, FLOWER_LONG_MAX);
    }

    private int readShortSide(JTextField longField, JTextField shortField, JCheckBox box) {
        int longSide = readLongSide(longField);
        if (box == null || !box.isSelected()) {
            return longSide;
        }
        int shortSide = parseSideField(shortField, 1, FLOWER_LONG_MAX);
        if (shortSide < 0 || longSide < 0 || shortSide > longSide) {
            return -1;
        }
        return shortSide;
    }

    /** Refresh credit hints when fast-mode toggles. */
    private void onFastModeToggled(boolean searchTab) {
        if (searchTab) {
            if (searchCreditLabel != null) {
                searchCreditLabel.setText(buildCreditText());
            }
        } else if (listSearchCreditLabel != null) {
            listSearchCreditLabel.setText(buildCreditText());
        }
    }

    /**
     * 获取合并后的 credit 文本
     */
    private String buildCreditText() {
        return getString("credit.text", buildCreditMemoryPhaseHtml());
    }

    /**
     * Localized string lookup.
     */
    private String getString(String key) {
        try {
            return messages.getString(key);
        } catch (Exception e) {
            return key; // missing key -> return key
        }
    }

    /**
     * 获取格式化字符串
     */
    private String getString(String key, Object... args) {
        try {
            return MessageFormat.format(messages.getString(key), args);
        } catch (Exception e) {
            return key; // missing key -> return key
        }
    }

    // 创建单种子搜索面板
    private JPanel createSingleSeedSearchPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 左侧：可滚动的设置区 + 固定的按钮/进度区

        // 输入区域
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        searchSeedLabel = new JLabel(getString("label.seed"));
        searchSeedLabel.setFont(getLoadedFont());
        inputPanel.add(searchSeedLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchSeedField = new JTextField("", 20);
        // 添加输入验证，非整数时提示
        searchSeedField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(searchSeedField, getString("label.seed").replace(":", ""));
            }
        });
        inputPanel.add(searchSeedField, gbc);

        // Thread Count 输入
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchThreadCountLabel = new JLabel(getString("label.threadCount"));
        searchThreadCountLabel.setFont(getLoadedFont());
        inputPanel.add(searchThreadCountLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchThreadCountField = new JTextField(String.valueOf(Runtime.getRuntime().availableProcessors()), 20);
        // 添加输入验证，非整数时提示
        searchThreadCountField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(searchThreadCountField, getString("label.threadCount").replace(":", ""));
            }
        });
        inputPanel.add(searchThreadCountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchLongSideLabel = new JLabel(getString("label.longSide"));
        searchLongSideLabel.setFont(getLoadedFont());
        inputPanel.add(searchLongSideLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchLongSideField = new JTextField(String.valueOf(FLOWER_LONG_DEFAULT), 20);
        rememberSide(searchLongSideField, FLOWER_LONG_DEFAULT);
        searchLongSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                commitLongSide(searchLongSideField, searchShortSideField);
            }
        });
        inputPanel.add(searchLongSideField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchShortSideCheckBox = new JCheckBox(getString("label.shortSide"), true);
        searchShortSideCheckBox.setFont(getLoadedFont());
        searchShortSideCheckBox.addActionListener(e -> applyShortSideEnable(true));
        inputPanel.add(searchShortSideCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchShortSideField = new JTextField(String.valueOf(FLOWER_SHORT_DEFAULT), 20);
        rememberSide(searchShortSideField, FLOWER_SHORT_DEFAULT);
        searchShortSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                commitShortSide(searchLongSideField, searchShortSideField);
            }
        });
        inputPanel.add(searchShortSideField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchViewDistanceLabel = new JLabel(getString("label.viewDistance"));
        searchViewDistanceLabel.setFont(getLoadedFont());
        inputPanel.add(searchViewDistanceLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchViewDistanceField = new JTextField(String.valueOf(FLOWER_VIEW_DEFAULT), 20);
        searchViewDistanceField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                applyViewDistance(searchViewDistanceField, searchLongSideField, searchShortSideCheckBox);
            }
        });
        inputPanel.add(searchViewDistanceField, gbc);

        // MinX 输入
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minXLabel = new JLabel(getString("label.minX"));
        minXLabel.setFont(getLoadedFont());
        inputPanel.add(minXLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minXField = new JTextField(String.valueOf(rangeMinForSide(SEARCH_RANGE_SIDE_LATEST)), 20);
        // 添加输入验证，非整数时提示
        minXField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(minXField, getString("label.minX").replace(":", ""));
            }
        });
        inputPanel.add(minXField, gbc);

        // MaxX 输入
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxXLabel = new JLabel(getString("label.maxX"));
        maxXLabel.setFont(getLoadedFont());
        inputPanel.add(maxXLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxXField = new JTextField(String.valueOf(rangeMaxForSide(SEARCH_RANGE_SIDE_LATEST)), 20);
        maxXField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(maxXField, getString("label.maxX").replace(":", ""));
            }
        });
        inputPanel.add(maxXField, gbc);

        // MinZ 输入
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minZLabel = new JLabel(getString("label.minZ"));
        minZLabel.setFont(getLoadedFont());
        inputPanel.add(minZLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minZField = new JTextField(String.valueOf(rangeMinForSide(SEARCH_RANGE_SIDE_LATEST)), 20);
        minZField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(minZField, getString("label.minZ").replace(":", ""));
            }
        });
        inputPanel.add(minZField, gbc);

        // MaxZ 输入
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxZLabel = new JLabel(getString("label.maxZ"));
        maxZLabel.setFont(getLoadedFont());
        inputPanel.add(maxZLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxZField = new JTextField(String.valueOf(rangeMaxForSide(SEARCH_RANGE_SIDE_LATEST)), 20);
        maxZField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(maxZField, getString("label.maxZ").replace(":", ""));
            }
        });
        inputPanel.add(maxZField, gbc);

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        squareSideCheckBox = new JCheckBox(getString("label.squareSide"));
        squareSideCheckBox.setFont(getLoadedFont());
        inputPanel.add(squareSideCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        squareSideField = new JTextField(String.valueOf(SEARCH_RANGE_SIDE_LATEST), 20);
        squareSideCheckBox.setSelected(true);
        squareSideCheckBox.addActionListener(e -> onSquareSideModeToggled());
        squareSideField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applySquareSideToBounds();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applySquareSideToBounds();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applySquareSideToBounds();
            }
        });
        squareSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(squareSideField, getString("label.squareSide").replace(":", ""));
            }
        });
        inputPanel.add(squareSideField, gbc);
        applyBoundFieldEnableState(true);
        applySquareSideToBounds();

        // Minecraft 版本
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchMcVersionLabel = new JLabel(getString("label.mcVersion"));
        searchMcVersionLabel.setFont(getLoadedFont());
        inputPanel.add(searchMcVersionLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchMcVersionComboBox = new JComboBox<>(SUPPORTED_MC_VERSIONS);
        searchMcVersionComboBox.setSelectedItem(DEFAULT_MC_VERSION);
        searchMcVersionComboBox.setFont(getLoadedFont());
        searchMcVersionComboBox.addActionListener(e -> onMcVersionChanged(true));
        inputPanel.add(searchMcVersionComboBox, gbc);

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchFastModeCheckBox = new JCheckBox(getString("label.fastMode"), true);
        searchFastModeCheckBox.setFont(getLoadedFont());
        searchFastModeCheckBox.addActionListener(e -> onFastModeToggled(true));
        inputPanel.add(searchFastModeCheckBox, gbc);
        gbc.gridwidth = 1;

        // 筛选条件：两种野花、玄武岩三角洲、横向/竖向
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchBothWildflowersCheckBox = new JCheckBox(getString("label.requireBothWildflowers"), false);
        searchBothWildflowersCheckBox.setFont(getLoadedFont());
        inputPanel.add(searchBothWildflowersCheckBox, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchBasaltCheckBox = new JCheckBox(getString("label.requireBasaltDeltas"), false);
        searchBasaltCheckBox.setFont(getLoadedFont());
        inputPanel.add(searchBasaltCheckBox, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchOrientationCheckBox = new JCheckBox(getString("label.requireOrientation"), false);
        searchOrientationCheckBox.setFont(getLoadedFont());
        searchOrientationCheckBox.addActionListener(e -> applyOrientationEnable(true));
        inputPanel.add(searchOrientationCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchOrientationComboBox = new JComboBox<>(new String[]{
                getString("label.orientationHorizontal"), getString("label.orientationVertical")});
        searchOrientationComboBox.setFont(getLoadedFont());
        searchOrientationComboBox.setSelectedIndex(0);
        inputPanel.add(searchOrientationComboBox, gbc);
        applyOrientationEnable(true);

        // 筛选条件：繁花森林离边距离（两处花梯度极端都须落在窗口中心区内）
        gbc.gridx = 0;
        gbc.gridy = 15;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchFlowerCenterCheckBox = new JCheckBox(getString("label.flowerCenterMargin"), false);
        searchFlowerCenterCheckBox.setFont(getLoadedFont());
        searchFlowerCenterCheckBox.setToolTipText(getString("tooltip.flowerCenterMargin"));
        searchFlowerCenterCheckBox.addActionListener(e -> applyFlowerCenterEnable(true));
        inputPanel.add(searchFlowerCenterCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        searchFlowerCenterField = new JTextField(String.valueOf(FLOWER_CENTER_MARGIN_DEFAULT), 6);
        searchFlowerCenterField.setFont(getLoadedFont());
        searchFlowerCenterField.setToolTipText(getString("tooltip.flowerCenterMargin"));
        searchFlowerCenterField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(searchFlowerCenterField,
                        getString("label.flowerCenterMargin").replace(":", ""));
            }
        });
        inputPanel.add(searchFlowerCenterField, gbc);
        applyFlowerCenterEnable(true);

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 16;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        searchLanguageLabel = new JLabel(getString("label.language"));
        searchLanguageLabel.setFont(getLoadedFont());
        inputPanel.add(searchLanguageLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        String[] languageOptions = {"中文", "English"};
        languageComboBox = new JComboBox<>(languageOptions);
        // 根据当前语言设置默认选项
        if (currentLocale.getLanguage().equals("zh")) {
            languageComboBox.setSelectedIndex(0);
        } else {
            languageComboBox.setSelectedIndex(1);
        }
        languageComboBox.addActionListener(e -> changeLanguage());
        inputPanel.add(languageComboBox, gbc);

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout());
        searchStartButton = new JButton(getString("button.startSearch"));
        searchPauseButton = new JButton(getString("button.pause"));
        searchStopButton = new JButton(getString("button.stop"));
        searchResetButton = new JButton(getString("button.reset"));
        searchResetToWorldBorderButton = new JButton(getString("button.resetToWorldBorder"));
        searchPauseButton.setEnabled(false);
        searchStopButton.setEnabled(false);
        buttonPanel.add(searchStartButton);
        buttonPanel.add(searchPauseButton);
        buttonPanel.add(searchStopButton);
        buttonPanel.add(searchResetButton);
        buttonPanel.add(searchResetToWorldBorderButton);

        // Hints / credit block.  BorderLayout (instead of FlowLayout) lets the
        // HTML label wrap to the viewport width, so long hints never force a
        // wider, clipped column.
        JPanel creditPanel = new JPanel(new BorderLayout());
        creditPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        searchCreditLabel = new JLabel(buildCreditText());
        searchCreditLabel.setFont(getLoadedFont());
        creditPanel.add(searchCreditLabel, BorderLayout.CENTER);

        // 进度区域
        JPanel progressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pgc = new GridBagConstraints();
        pgc.insets = new Insets(5, 5, 5, 5);
        pgc.anchor = GridBagConstraints.WEST;
        pgc.fill = GridBagConstraints.HORIZONTAL;
        pgc.weightx = 1.0;

        pgc.gridx = 0;
        pgc.gridy = 0;
        pgc.gridwidth = 2;
        searchProgressBar = new JProgressBar(0, 100);
        searchProgressBar.setStringPainted(true);
        searchProgressBar.setString(getString("progress.format", 0, 0, 0.0));
        progressPanel.add(searchProgressBar, pgc);

        pgc.gridwidth = 1;
        pgc.gridy = 1;
        searchElapsedTimeLabel = new JLabel(getString("elapsedTime", formatTime(0)));
        progressPanel.add(searchElapsedTimeLabel, pgc);

        pgc.gridy = 3;
        searchRemainingTimeLabel = new JLabel(getString("remainingTime.calculating"));
        progressPanel.add(searchRemainingTimeLabel, pgc);

        // Settings column: inputs + hints scroll vertically; the action buttons
        // and the progress area are pinned below the viewport, so a long hint
        // list can never cover or push away Start / Pause / Stop.
        JPanel scrollContent = new ScrollablePanel(new BorderLayout());
        scrollContent.add(inputPanel, BorderLayout.NORTH);
        scrollContent.add(creditPanel, BorderLayout.CENTER);

        JScrollPane settingsScroll = new JScrollPane(scrollContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScroll.setBorder(null);
        settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        settingsScroll.setPreferredSize(new Dimension(700, 600));

        JPanel leftBottomPanel = new JPanel(new BorderLayout());
        leftBottomPanel.add(buttonPanel, BorderLayout.NORTH);
        leftBottomPanel.add(progressPanel, BorderLayout.CENTER);

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(settingsScroll, BorderLayout.CENTER);
        leftContainer.add(leftBottomPanel, BorderLayout.SOUTH);

        // 右侧：结果显示
        searchRightPanel = new JPanel(new BorderLayout());
        searchRightPanel.setBorder(BorderFactory.createTitledBorder(getString("result.border")));
        searchResultArea = new JTextArea();
        searchResultArea.setEditable(false);
        searchResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(searchResultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel exportSortPanel = new JPanel(new FlowLayout());
        searchExportButton = new JButton(getString("button.export"));
        searchExportButton.addActionListener(e -> exportSearchResults());
        searchSortButton = new JButton(getString("button.sort"));
        searchSortButton.addActionListener(e -> sortSearchResults());
        exportSortPanel.add(searchExportButton);
        exportSortPanel.add(searchSortButton);
        searchRightPanel.add(scrollPane, BorderLayout.CENTER);
        searchRightPanel.add(exportSortPanel, BorderLayout.SOUTH);

        // 使用 JSplitPane 分割
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, searchRightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 添加事件监听
        searchStartButton.addActionListener(e -> startSearch());
        searchPauseButton.addActionListener(e -> toggleSearchPause());
        searchStopButton.addActionListener(e -> stopSearch());
        searchResetButton.addActionListener(e -> resetSearchToDefaults());
        searchResetToWorldBorderButton.addActionListener(e -> resetSearchToWorldBorder());

        // 添加输入字段监听，检测参数变化
        addSearchParameterListeners();

        return mainPanel;
    }


    // 添加搜索参数监听器，检测参数变化（不包括线程数）
    private void addSearchParameterListeners() {
        // 种子变化监听（在已有监听器基础上添加检查）
        searchSeedField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }
        });

        // 坐标变化监听
        minXField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }
        });
        maxXField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }
        });
        minZField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }
        });
        maxZField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkSearchParameterChange();
            }
        });
    }

    // 检查搜索参数是否变化
    private void checkSearchParameterChange() {
        if (isSearchRunning && !isSearchPaused) {
            return; // running and not paused
        }

        if (!isSearchPaused) {
            return; // not paused
        }

        try {
            String seedText = searchSeedField.getText().trim();
            if (seedText.isEmpty()) {
                return;
            }
            long seed = Long.parseLong(seedText);
            int minArea = readLongSide(searchLongSideField);
            if (minArea < 0) minArea = 0;
            int minX = Integer.parseInt(minXField.getText().trim());
            int maxX = Integer.parseInt(maxXField.getText().trim());
            int minZ = Integer.parseInt(minZField.getText().trim());
            int maxZ = Integer.parseInt(maxZField.getText().trim());

            // 如果参数变化且处于暂停状态，重置进度（线程数变化不触发重置）
            if (minArea < 0) minArea = 0;
            if (seed != lastSearchSeed || minX != lastSearchMinX ||
                    maxX != lastSearchMaxX || minZ != lastSearchMinZ || maxZ != lastSearchMaxZ ||
                    minArea != lastSearchMinArea) {
                if (searchRunner != null) {
                    searchRunner.stop();
                }
                isSearchRunning = false;
                isSearchPaused = false;
                searchStartButton.setEnabled(true);
                searchPauseButton.setEnabled(false);
                searchPauseButton.setText(getString("button.pause"));
                searchStopButton.setEnabled(false);
                searchResetButton.setEnabled(true);
                if (searchResetToWorldBorderButton != null) searchResetToWorldBorderButton.setEnabled(true);
                searchSeedField.setEnabled(true);
                searchThreadCountField.setEnabled(true);
                if (searchFastModeCheckBox != null) searchFastModeCheckBox.setEnabled(true);
                if (searchMcVersionComboBox != null) searchMcVersionComboBox.setEnabled(true);
                enableSideFields(true);
                applyBoundFieldEnableState(true);
                if (languageComboBox != null) {
                    languageComboBox.setEnabled(true);
                }
                searchResultArea.setText("");
                searchProgressBar.setIndeterminate(false);
                searchProgressBar.setValue(0);
                searchProgressBar.setString(getString("progress.format", 0, 0, 0.0));
                searchRemainingTimeLabel.setText(getString("remainingTime.reset"));
            }
        } catch (NumberFormatException e) {
            // 忽略无效输入
        }
    }


    // 验证整数输入
    private void validateIntegerInput(JTextField field, String fieldName) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return; // empty: validate when search starts
        }
        try {
            // Parse as double and check integer
            double value = Double.parseDouble(text);
            if (value != Math.floor(value)) {
                JOptionPane.showMessageDialog(this, getString("validation.integerRequired", fieldName), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                field.requestFocus();
            }
        } catch (NumberFormatException e) {
            // 不是数字，会在开始运行时验证
        }
    }

    private long parseDistanceSq(String line) {
        if (line == null || !line.startsWith("/tp ")) {
            return Long.MAX_VALUE;
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 4) {
            return Long.MAX_VALUE;
        }
        try {
            long x = Long.parseLong(parts[1]);
            long z = Long.parseLong(parts[3]);
            return x * x + z * z;
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /** Result lines mark cherry grove as {@code pink_petals} / {@code pink_petals(1.19.4+)}. */
    private static boolean hasPinkPetals(String line) {
        return line != null && line.contains("pink_petals");
    }

    /** Cherry first, then nearer to origin. */
    private int compareTpByCherryThenDistance(String a, String b) {
        boolean pa = hasPinkPetals(a);
        boolean pb = hasPinkPetals(b);
        if (pa != pb) {
            return pa ? -1 : 1;
        }
        return Long.compare(parseDistanceSq(a), parseDistanceSq(b));
    }

    private void sortSearchResults() {
        String text = searchResultArea.getText().trim();
        if (text.isEmpty()) return;
        String[] lines = text.split("\n");
        List<String> withArea = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("/tp ")) {
                withArea.add(line);
            } else {
                other.add(line);
            }
        }
        withArea.sort(this::compareTpByCherryThenDistance);
        StringBuilder sb = new StringBuilder();
        for (String s : withArea) sb.append(s).append("\n");
        for (String s : other) sb.append(s).append("\n");
        searchResultArea.setText(sb.toString());
    }

    // 搜索相关方法（河流多阶段搜索）
    private void startSearch() {
        if (isSearchRunning && isSearchPaused) {
            if (searchRunner != null) {
                searchRunner.resume();
                isSearchPaused = false;
                searchPauseButton.setText(getString("button.pause"));
            }
            return;
        }
        if (FlowerFarmFinderSearchRunner.isAnySearchActive()) {
            JOptionPane.showMessageDialog(this, getString("error.searchBusy"), getString("prompt.error"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 验证种子
            String seedText = searchSeedField.getText().trim();
            if (seedText.isEmpty()) {
                JOptionPane.showMessageDialog(this, getString("error.seedRequired"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查种子是否为整数
            double seedDouble;
            try {
                seedDouble = Double.parseDouble(seedText);
                if (seedDouble != Math.floor(seedDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.seedMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.seedFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查种子是否超过MC正常种子边界（绝对值超过2^63-1）
            long seed;
            try {
                seed = Long.parseLong(seedText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.seedOutOfRange"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 验证线程数
            String threadText = searchThreadCountField.getText().trim();
            double threadDouble;
            try {
                threadDouble = Double.parseDouble(threadText);
                if (threadDouble != Math.floor(threadDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.threadCountMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.threadCountFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            int threadCount = (int) threadDouble;
            if (threadCount < 1) {
                JOptionPane.showMessageDialog(this, getString("error.threadCountRequired"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查线程数是否超过CPU核数
            int cpuThreads = Runtime.getRuntime().availableProcessors();
            if (threadCount > cpuThreads) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        getString("error.threadCountExceedsCPU", cpuThreads, cpuThreads),
                        getString("prompt.adjustThreadCount"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (result == JOptionPane.YES_OPTION) {
                    threadCount = cpuThreads;
                    searchThreadCountField.setText(String.valueOf(cpuThreads));
                } else {
                    return;
                }
            }

            boolean fastMode = isSearchFastMode();
            boolean bothWildflowers = isBothWildflowersFilter(searchBothWildflowersCheckBox);
            boolean basaltDeltas = isBasaltFilter(searchBasaltCheckBox);
            int orientation = selectedOrientation(searchOrientationCheckBox, searchOrientationComboBox);
            String mcVersion = selectedMcVersion(searchMcVersionComboBox);
            int longSide = readLongSide(searchLongSideField);
            if (longSide < 0) {
                JOptionPane.showMessageDialog(this, getString("error.longSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean squareOnly = searchShortSideCheckBox == null || !searchShortSideCheckBox.isSelected();
            int shortSide = readShortSide(searchLongSideField, searchShortSideField, searchShortSideCheckBox);
            if (shortSide < 0) {
                JOptionPane.showMessageDialog(this, getString("error.shortSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            int demandSideX = longSide;
            int demandSideZ = squareOnly ? longSide : shortSide;
            int flowerCenterMargin = readFlowerCenterMargin(searchFlowerCenterCheckBox, searchFlowerCenterField,
                    demandSideX, demandSideZ);
            if (reportFlowerCenterMarginError(flowerCenterMargin, demandSideX, demandSideZ)) {
                return;
            }

            // 验证XZ坐标
            String minXText = minXField.getText().trim();
            String maxXText = maxXField.getText().trim();
            String minZText = minZField.getText().trim();
            String maxZText = maxZField.getText().trim();

            // 检查是否为整数
            double minXDouble, maxXDouble, minZDouble, maxZDouble;
            try {
                minXDouble = Double.parseDouble(minXText);
                if (minXDouble != Math.floor(minXDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.minXMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    minXField.setText(String.valueOf(rangeMinForSide(defaultSearchRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.minXFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                minXField.setText(String.valueOf(rangeMinForSide(defaultSearchRangeSide())));
                return;
            }

            try {
                maxXDouble = Double.parseDouble(maxXText);
                if (maxXDouble != Math.floor(maxXDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.maxXMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    maxXField.setText(String.valueOf(rangeMaxForSide(defaultSearchRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.maxXFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                maxXField.setText(String.valueOf(rangeMaxForSide(defaultSearchRangeSide())));
                return;
            }

            try {
                minZDouble = Double.parseDouble(minZText);
                if (minZDouble != Math.floor(minZDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.minZMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    minZField.setText(String.valueOf(rangeMinForSide(defaultSearchRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.minZFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                minZField.setText(String.valueOf(rangeMinForSide(defaultSearchRangeSide())));
                return;
            }

            try {
                maxZDouble = Double.parseDouble(maxZText);
                if (maxZDouble != Math.floor(maxZDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.maxZMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    maxZField.setText(String.valueOf(rangeMaxForSide(defaultSearchRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.maxZFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                maxZField.setText(String.valueOf(rangeMaxForSide(defaultSearchRangeSide())));
                return;
            }

            int minX = (int) minXDouble;
            int maxX = (int) maxXDouble;
            int minZ = (int) minZDouble;
            int maxZ = (int) maxZDouble;

            if (minX >= maxX) {
                JOptionPane.showMessageDialog(this, getString("error.minXGreaterThanMaxX"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (minZ >= maxZ) {
                JOptionPane.showMessageDialog(this, getString("error.minZGreaterThanMaxZ"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            long searchArea = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
            if (searchArea > FlowerFarmFinderSearchRunner.MAX_SEARCH_AREA_BLOCKS) {
                JOptionPane.showMessageDialog(this, getString("error.searchAreaTooLarge"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查世界边界：minX < -30000000, maxX > 30000000, minZ < -30000000, maxZ > 30000000
            boolean outOfBounds = minX < BOUNDARY_MIN_X || maxX > BOUNDARY_MAX_X || minZ < BOUNDARY_MIN_Z || maxZ > BOUNDARY_MAX_Z;

            if (outOfBounds) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        getString("error.outOfBounds"),
                        getString("prompt.warning"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            lastSearchSeed = seed;
            lastSearchMinX = minX;
            lastSearchMaxX = maxX;
            lastSearchMinZ = minZ;
            lastSearchMaxZ = maxZ;
            lastSearchMinArea = longSide;
            lastSearchThreadCount = threadCount;

            isSearchRunning = true;
            isSearchPaused = false;
            searchStartButton.setEnabled(false);
            searchPauseButton.setEnabled(true);
            searchPauseButton.setText(getString("button.pause"));
            searchStopButton.setEnabled(true);
            searchResetButton.setEnabled(false);
            if (searchResetToWorldBorderButton != null) searchResetToWorldBorderButton.setEnabled(false);
            searchSeedField.setEnabled(false);
            searchThreadCountField.setEnabled(false);
            searchLongSideField.setEnabled(false);
            searchShortSideField.setEnabled(false);
            searchViewDistanceField.setEnabled(false);
            if (searchFastModeCheckBox != null) searchFastModeCheckBox.setEnabled(false);
            if (searchBothWildflowersCheckBox != null) searchBothWildflowersCheckBox.setEnabled(false);
            if (searchBasaltCheckBox != null) searchBasaltCheckBox.setEnabled(false);
            if (searchOrientationCheckBox != null) searchOrientationCheckBox.setEnabled(false);
            if (searchOrientationComboBox != null) searchOrientationComboBox.setEnabled(false);
            if (searchFlowerCenterCheckBox != null) searchFlowerCenterCheckBox.setEnabled(false);
            if (searchFlowerCenterField != null) searchFlowerCenterField.setEnabled(false);
            if (searchMcVersionComboBox != null) searchMcVersionComboBox.setEnabled(false);
            applyBoundFieldEnableState(false);
            if (languageComboBox != null) languageComboBox.setEnabled(false);
            final long resultToken = singleSearchResultToken.incrementAndGet();
            searchResultArea.setText("");
            searchProgressBar.setIndeterminate(true);
            searchProgressBar.setValue(0);
            searchProgressBar.setString(getString("remainingTime.calculating"));
            searchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(0)));
            searchRemainingTimeLabel.setText(getString("remainingTime.calculating"));

            searchRunner = new FlowerFarmFinderSearchRunner();
            if (searchShortSideCheckBox != null) searchShortSideCheckBox.setEnabled(false);
            if (!searchRunner.startFlowerSearch(seed, minX, maxX, minZ, maxZ, longSide, shortSide, squareOnly,
                threadCount, fastMode, bothWildflowers, basaltDeltas, orientation, flowerCenterMargin, mcVersion,
                info -> updateCaveSearchProgress(resultToken, info),
                line -> addSearchResult(resultToken, line))) {
                isSearchRunning = false;
                enableSearchControls();
                JOptionPane.showMessageDialog(this, getString("error.searchBusy"), getString("prompt.error"), JOptionPane.WARNING_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, getString("error.invalidNumber"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleSearchPause() {
        if (searchRunner == null || !isSearchRunning) return;
        if (isSearchPaused) {
            searchRunner.resume();
            isSearchPaused = false;
            searchPauseButton.setText(getString("button.pause"));
        } else {
            searchRunner.pause();
            isSearchPaused = true;
            searchPauseButton.setText(getString("button.resume"));
        }
        // 运行中（含暂停）不允许改线程数；JNI 线程数仅在开始搜索时传入
        searchThreadCountField.setEnabled(false);
    }

    private void stopSearch() {
        if (searchRunner == null || !isSearchRunning) {
            return;
        }
        searchRunner.stop();
        singleSearchResultToken.incrementAndGet();
        enableSearchControls();
        if (searchProgressBar != null) {
            searchProgressBar.setIndeterminate(false);
        }
        searchRemainingTimeLabel.setText(getString("remainingTime.stopped"));
    }

    private void resetSearchToDefaults() {
        applyDefaultSearchRange();
    }

    private void resetSearchToWorldBorder() {
        applySearchBounds(BOUNDARY_MIN_X, BOUNDARY_MAX_X, BOUNDARY_MIN_Z, BOUNDARY_MAX_Z);
    }

    private void applySearchBounds(int minX, int maxX, int minZ, int maxZ) {
        long side = (long) maxX - minX + 1;
        if (squareSideCheckBox != null) {
            squareSideCheckBox.setSelected(true);
        }
        if (squareSideField != null) {
            squareSideUpdating = true;
            squareSideField.setText(String.valueOf(side));
            squareSideUpdating = false;
        }
        minXField.setText(String.valueOf(minX));
        maxXField.setText(String.valueOf(maxX));
        minZField.setText(String.valueOf(minZ));
        maxZField.setText(String.valueOf(maxZ));
        applyBoundFieldEnableState(!isSearchRunning);
        applySquareSideToBounds();
    }

    private void onSquareSideModeToggled() {
        applyBoundFieldEnableState(!isSearchRunning);
        if (squareSideCheckBox != null && squareSideCheckBox.isSelected()) {
            applySquareSideToBounds();
        }
    }

    private void applyBoundFieldEnableState(boolean allowEdit) {
        boolean squareMode = squareSideCheckBox != null && squareSideCheckBox.isSelected();
        if (squareSideCheckBox != null) {
            squareSideCheckBox.setEnabled(allowEdit);
        }
        if (squareSideField != null) {
            squareSideField.setEnabled(allowEdit && squareMode);
        }
        boolean boundsEditable = allowEdit && !squareMode;
        if (minXField != null) {
            minXField.setEnabled(boundsEditable);
        }
        if (maxXField != null) {
            maxXField.setEnabled(boundsEditable);
        }
        if (minZField != null) {
            minZField.setEnabled(boundsEditable);
        }
        if (maxZField != null) {
            maxZField.setEnabled(boundsEditable);
        }
    }

    /** Fill min/max from square side length centered at (0, 0). Side = max - min + 1. */
    private void applySquareSideToBounds() {
        if (squareSideUpdating || squareSideCheckBox == null || !squareSideCheckBox.isSelected()) {
            return;
        }
        if (squareSideField == null || minXField == null) {
            return;
        }
        String text = squareSideField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            long side = Long.parseLong(text);
            if (side < 1) {
                return;
            }
            long min = -side / 2;
            long max = min + side - 1;
            if (min < Integer.MIN_VALUE || max > Integer.MAX_VALUE) {
                return;
            }
            squareSideUpdating = true;
            minXField.setText(String.valueOf((int) min));
            maxXField.setText(String.valueOf((int) max));
            minZField.setText(String.valueOf((int) min));
            maxZField.setText(String.valueOf((int) max));
            squareSideUpdating = false;
            checkSearchParameterChange();
        } catch (NumberFormatException ignored) {
            // wait until the field holds a valid integer
        }
    }

    private void onListSquareSideModeToggled() {
        applyListBoundFieldEnableState(!isListSearchRunning);
        if (listSquareSideCheckBox != null && listSquareSideCheckBox.isSelected()) {
            applyListSquareSideToBounds();
        }
    }

    private void applyListBoundFieldEnableState(boolean allowEdit) {
        boolean squareMode = listSquareSideCheckBox != null && listSquareSideCheckBox.isSelected();
        if (listSquareSideCheckBox != null) {
            listSquareSideCheckBox.setEnabled(allowEdit);
        }
        if (listSquareSideField != null) {
            listSquareSideField.setEnabled(allowEdit && squareMode);
        }
        boolean boundsEditable = allowEdit && !squareMode;
        if (listMinXField != null) {
            listMinXField.setEnabled(boundsEditable);
        }
        if (listMaxXField != null) {
            listMaxXField.setEnabled(boundsEditable);
        }
        if (listMinZField != null) {
            listMinZField.setEnabled(boundsEditable);
        }
        if (listMaxZField != null) {
            listMaxZField.setEnabled(boundsEditable);
        }
    }

    private void applyListSquareSideToBounds() {
        if (listSquareSideUpdating || listSquareSideCheckBox == null || !listSquareSideCheckBox.isSelected()) {
            return;
        }
        if (listSquareSideField == null || listMinXField == null) {
            return;
        }
        String text = listSquareSideField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            long side = Long.parseLong(text);
            if (side < 1) {
                return;
            }
            long min = -side / 2;
            long max = min + side - 1;
            if (min < Integer.MIN_VALUE || max > Integer.MAX_VALUE) {
                return;
            }
            listSquareSideUpdating = true;
            listMinXField.setText(String.valueOf((int) min));
            listMaxXField.setText(String.valueOf((int) max));
            listMinZField.setText(String.valueOf((int) min));
            listMaxZField.setText(String.valueOf((int) max));
            listSquareSideUpdating = false;
            checkListSearchParameterChange();
        } catch (NumberFormatException ignored) {
            // wait until the field holds a valid integer
        }
    }

    // 创建从种子列表搜索面板
    private JPanel createListSearchPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 左侧：可滚动的设置区 + 固定的按钮/进度区

        // 输入区域
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Seed 文件选择
        gbc.gridx = 0;
        gbc.gridy = 0;
        listSearchSeedFileTitleLabel = new JLabel(getString("label.seedFile"));
        listSearchSeedFileTitleLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchSeedFileTitleLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel seedFilePanel = new JPanel(new BorderLayout());
        listSearchSeedFileButton = new JButton(getString("button.selectFile"));
        listSearchSeedFileButton.addActionListener(e -> selectSeedFile());
        listSearchSeedFileLabel = new JLabel(getString("label.noFileSelected"));
        listSearchSeedFileLabel.setFont(getLoadedFont());
        seedFilePanel.add(listSearchSeedFileButton, BorderLayout.WEST);
        seedFilePanel.add(listSearchSeedFileLabel, BorderLayout.CENTER);
        inputPanel.add(seedFilePanel, gbc);

        // Thread Count 输入
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSearchThreadCountLabel = new JLabel(getString("label.threadCount"));
        listSearchThreadCountLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchThreadCountLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listSearchThreadCountField = new JTextField(String.valueOf(Runtime.getRuntime().availableProcessors()), 20);
        listSearchThreadCountField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listSearchThreadCountField, getString("label.threadCount").replace(":", ""));
            }
        });
        inputPanel.add(listSearchThreadCountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listLongSideLabel = new JLabel(getString("label.longSide"));
        listLongSideLabel.setFont(getLoadedFont());
        inputPanel.add(listLongSideLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listLongSideField = new JTextField(String.valueOf(FLOWER_LONG_DEFAULT), 20);
        rememberSide(listLongSideField, FLOWER_LONG_DEFAULT);
        listLongSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                commitLongSide(listLongSideField, listShortSideField);
            }
        });
        inputPanel.add(listLongSideField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listShortSideCheckBox = new JCheckBox(getString("label.shortSide"), true);
        listShortSideCheckBox.setFont(getLoadedFont());
        listShortSideCheckBox.addActionListener(e -> applyShortSideEnable(false));
        inputPanel.add(listShortSideCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listShortSideField = new JTextField(String.valueOf(FLOWER_SHORT_DEFAULT), 20);
        rememberSide(listShortSideField, FLOWER_SHORT_DEFAULT);
        listShortSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                commitShortSide(listLongSideField, listShortSideField);
            }
        });
        inputPanel.add(listShortSideField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listViewDistanceLabel = new JLabel(getString("label.viewDistance"));
        listViewDistanceLabel.setFont(getLoadedFont());
        inputPanel.add(listViewDistanceLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listViewDistanceField = new JTextField(String.valueOf(FLOWER_VIEW_DEFAULT), 20);
        listViewDistanceField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                applyViewDistance(listViewDistanceField, listLongSideField, listShortSideCheckBox);
            }
        });
        inputPanel.add(listViewDistanceField, gbc);

        // MinX 输入
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSearchMinXLabel = new JLabel(getString("label.minX"));
        listSearchMinXLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchMinXLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listMinXField = new JTextField(String.valueOf(rangeMinForSide(LIST_RANGE_SIDE_LATEST)), 20);
        listMinXField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listMinXField, getString("label.minX").replace(":", ""));
            }
        });
        inputPanel.add(listMinXField, gbc);

        // MaxX 输入
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSearchMaxXLabel = new JLabel(getString("label.maxX"));
        listSearchMaxXLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchMaxXLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listMaxXField = new JTextField(String.valueOf(rangeMaxForSide(LIST_RANGE_SIDE_LATEST)), 20);
        listMaxXField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listMaxXField, getString("label.maxX").replace(":", ""));
            }
        });
        inputPanel.add(listMaxXField, gbc);

        // MinZ 输入
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSearchMinZLabel = new JLabel(getString("label.minZ"));
        listSearchMinZLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchMinZLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listMinZField = new JTextField(String.valueOf(rangeMinForSide(LIST_RANGE_SIDE_LATEST)), 20);
        listMinZField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listMinZField, getString("label.minZ").replace(":", ""));
            }
        });
        inputPanel.add(listMinZField, gbc);

        // MaxZ 输入
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSearchMaxZLabel = new JLabel(getString("label.maxZ"));
        listSearchMaxZLabel.setFont(getLoadedFont());
        inputPanel.add(listSearchMaxZLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listMaxZField = new JTextField(String.valueOf(rangeMaxForSide(LIST_RANGE_SIDE_LATEST)), 20);
        listMaxZField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listMaxZField, getString("label.maxZ").replace(":", ""));
            }
        });
        inputPanel.add(listMaxZField, gbc);

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listSquareSideCheckBox = new JCheckBox(getString("label.squareSide"));
        listSquareSideCheckBox.setFont(getLoadedFont());
        inputPanel.add(listSquareSideCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listSquareSideField = new JTextField(String.valueOf(LIST_RANGE_SIDE_LATEST), 20);
        listSquareSideCheckBox.setSelected(true);
        listSquareSideCheckBox.addActionListener(e -> onListSquareSideModeToggled());
        listSquareSideField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyListSquareSideToBounds();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyListSquareSideToBounds();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyListSquareSideToBounds();
            }
        });
        listSquareSideField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listSquareSideField, getString("label.squareSide").replace(":", ""));
            }
        });
        inputPanel.add(listSquareSideField, gbc);
        applyListBoundFieldEnableState(true);
        applyListSquareSideToBounds();

        // Minecraft 版本
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listMcVersionLabel = new JLabel(getString("label.mcVersion"));
        listMcVersionLabel.setFont(getLoadedFont());
        inputPanel.add(listMcVersionLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listMcVersionComboBox = new JComboBox<>(SUPPORTED_MC_VERSIONS);
        listMcVersionComboBox.setSelectedItem(DEFAULT_MC_VERSION);
        listMcVersionComboBox.setFont(getLoadedFont());
        listMcVersionComboBox.addActionListener(e -> onMcVersionChanged(false));
        inputPanel.add(listMcVersionComboBox, gbc);

        // Seed 输入
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listFastModeCheckBox = new JCheckBox(getString("label.fastMode"), true);
        listFastModeCheckBox.setFont(getLoadedFont());
        listFastModeCheckBox.addActionListener(e -> onFastModeToggled(false));
        inputPanel.add(listFastModeCheckBox, gbc);
        gbc.gridwidth = 1;

        // 筛选条件：两种野花、玄武岩三角洲、横向/竖向
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listBothWildflowersCheckBox = new JCheckBox(getString("label.requireBothWildflowers"), false);
        listBothWildflowersCheckBox.setFont(getLoadedFont());
        inputPanel.add(listBothWildflowersCheckBox, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listBasaltCheckBox = new JCheckBox(getString("label.requireBasaltDeltas"), false);
        listBasaltCheckBox.setFont(getLoadedFont());
        inputPanel.add(listBasaltCheckBox, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listOrientationCheckBox = new JCheckBox(getString("label.requireOrientation"), false);
        listOrientationCheckBox.setFont(getLoadedFont());
        listOrientationCheckBox.addActionListener(e -> applyOrientationEnable(false));
        inputPanel.add(listOrientationCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listOrientationComboBox = new JComboBox<>(new String[]{
                getString("label.orientationHorizontal"), getString("label.orientationVertical")});
        listOrientationComboBox.setFont(getLoadedFont());
        listOrientationComboBox.setSelectedIndex(0);
        inputPanel.add(listOrientationComboBox, gbc);
        applyOrientationEnable(false);

        // 筛选条件：繁花森林离边距离（两处花梯度极端都须落在窗口中心区内）
        gbc.gridx = 0;
        gbc.gridy = 15;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        listFlowerCenterCheckBox = new JCheckBox(getString("label.flowerCenterMargin"), false);
        listFlowerCenterCheckBox.setFont(getLoadedFont());
        listFlowerCenterCheckBox.setToolTipText(getString("tooltip.flowerCenterMargin"));
        listFlowerCenterCheckBox.addActionListener(e -> applyFlowerCenterEnable(false));
        inputPanel.add(listFlowerCenterCheckBox, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        listFlowerCenterField = new JTextField(String.valueOf(FLOWER_CENTER_MARGIN_DEFAULT), 6);
        listFlowerCenterField.setFont(getLoadedFont());
        listFlowerCenterField.setToolTipText(getString("tooltip.flowerCenterMargin"));
        listFlowerCenterField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateIntegerInput(listFlowerCenterField,
                        getString("label.flowerCenterMargin").replace(":", ""));
            }
        });
        inputPanel.add(listFlowerCenterField, gbc);
        applyFlowerCenterEnable(false);

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout());
        listSearchStartButton = new JButton(getString("button.startSearch"));
        listSearchPauseButton = new JButton(getString("button.pause"));
        listSearchStopButton = new JButton(getString("button.stop"));
        listSearchResetButton = new JButton(getString("button.resetList"));
        listSearchPauseButton.setEnabled(false);
        listSearchStopButton.setEnabled(false);
        buttonPanel.add(listSearchStartButton);
        buttonPanel.add(listSearchPauseButton);
        buttonPanel.add(listSearchStopButton);
        buttonPanel.add(listSearchResetButton);

        // Hints / credit block: wraps to the viewport width (see single-seed tab).
        JPanel creditPanel = new JPanel(new BorderLayout());
        creditPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        listSearchCreditLabel = new JLabel(buildCreditText());
        listSearchCreditLabel.setFont(getLoadedFont());
        creditPanel.add(listSearchCreditLabel, BorderLayout.CENTER);

        // 进度区域
        JPanel progressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pgc = new GridBagConstraints();
        pgc.insets = new Insets(5, 5, 5, 5);
        pgc.anchor = GridBagConstraints.WEST;
        pgc.fill = GridBagConstraints.HORIZONTAL;
        pgc.weightx = 1.0;

        pgc.gridx = 0;
        pgc.gridy = 0;
        pgc.gridwidth = 2;
        listSearchProgressBar = new JProgressBar(0, 100);
        listSearchProgressBar.setStringPainted(true);
        listSearchProgressBar.setString(getString("progress.total", 0, 0, 0.0));
        progressPanel.add(listSearchProgressBar, pgc);

        pgc.gridwidth = 1;
        pgc.gridy = 1;
        listSearchElapsedTimeLabel = new JLabel(getString("elapsedTime", formatTime(0)));
        progressPanel.add(listSearchElapsedTimeLabel, pgc);

        pgc.gridy = 2;
        listSearchCurrentSeedProgressLabel = new JLabel(getString("currentSeed.default"));
        listSearchCurrentSeedProgressLabel.setFont(getLoadedFont());
        progressPanel.add(listSearchCurrentSeedProgressLabel, pgc);

        pgc.gridy = 3;
        listSearchRemainingTimeLabel = new JLabel(getString("remainingTime.calculating"));
        progressPanel.add(listSearchRemainingTimeLabel, pgc);

        // Settings column: inputs + hints scroll; buttons and progress stay pinned.
        JPanel scrollContent = new ScrollablePanel(new BorderLayout());
        scrollContent.add(inputPanel, BorderLayout.NORTH);
        scrollContent.add(creditPanel, BorderLayout.CENTER);

        JScrollPane settingsScroll = new JScrollPane(scrollContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScroll.setBorder(null);
        settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        settingsScroll.setPreferredSize(new Dimension(700, 600));

        JPanel leftBottomPanel = new JPanel(new BorderLayout());
        leftBottomPanel.add(buttonPanel, BorderLayout.NORTH);
        leftBottomPanel.add(progressPanel, BorderLayout.CENTER);

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(settingsScroll, BorderLayout.CENTER);
        leftContainer.add(leftBottomPanel, BorderLayout.SOUTH);

        // 右侧：结果显示
        listSearchRightPanel = new JPanel(new BorderLayout());
        listSearchRightPanel.setBorder(BorderFactory.createTitledBorder(getString("result.border")));
        listSearchResultArea = new JTextArea();
        listSearchResultArea.setEditable(false);
        listSearchResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(listSearchResultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel exportPanel = new JPanel(new FlowLayout());
        listSearchExportButton = new JButton(getString("button.export"));
        listSearchExportButton.addActionListener(e -> exportListSearchResults());
        listSearchExportSeedListButton = new JButton(getString("button.exportSeedList"));
        listSearchExportSeedListButton.addActionListener(e -> exportSeedList());
        listSortButton = new JButton(getString("button.sort"));
        listSortButton.addActionListener(e -> sortListResults());
        exportPanel.add(listSearchExportButton);
        exportPanel.add(listSearchExportSeedListButton);
        exportPanel.add(listSortButton);
        listSearchRightPanel.add(scrollPane, BorderLayout.CENTER);
        listSearchRightPanel.add(exportPanel, BorderLayout.SOUTH);

        // 使用 JSplitPane 分割
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, listSearchRightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 添加事件监听
        listSearchStartButton.addActionListener(e -> startListSearch());
        listSearchPauseButton.addActionListener(e -> toggleListSearchPause());
        listSearchStopButton.addActionListener(e -> stopListSearch());
        listSearchResetButton.addActionListener(e -> resetListSearchToDefaults());

        // 添加输入字段监听，检测参数变化
        addListSearchParameterListeners();

        return mainPanel;
    }

    private void updateCaveSearchProgress(long resultToken, FlowerFarmFinderSearchRunner.ProgressInfo info) {
        SwingUtilities.invokeLater(() -> {
            if (resultToken != singleSearchResultToken.get()) {
                return;
            }
            if (info.done()) {
                if (resultToken != singleSearchResultToken.get()) {
                    return;
                }
                singleSearchResultToken.incrementAndGet();
                if (!isSearchRunning) {
                    return;
                }
                enableSearchControls();
                searchProgressBar.setIndeterminate(false);
                searchProgressBar.setValue(100);
                searchProgressBar.setString(getString("progress.complete", info.processed(), info.total()));
                searchRemainingTimeLabel.setText(getString("remainingTime.completed"));
                return;
            }
            if (!isSearchRunning) return;

            int phase = info.phase();
            // Phase 2 now reports its own candidate-hit counters, so it gets a
            // numeric bar like the other phases; only an unknown phase is shown
            // as indeterminate.
            boolean unknownPhase = (phase == -1);
            searchProgressBar.setIndeterminate(unknownPhase);
            if (!unknownPhase) {
                searchProgressBar.setValue((int) Math.min(100, info.percentage()));
            }
            String stageKey = (phase == PHASE_PREPARING) ? "progress.stage0"
                    : (phase == PHASE_MERGE) ? "progress.stage2"
                    : (phase == PHASE_VERIFY) ? "progress.stage3" : "progress.stage1";
            searchProgressBar.setString(unknownPhase
                    ? getString(stageKey, 0, 0, 0.0)
                    : getString(stageKey, info.processed(), info.total(), info.percentage()));

            searchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(info.elapsedMs())));
            if (isSearchPaused) {
                searchRemainingTimeLabel.setText(info.nativePauseSettled()
                        ? getString("remainingTime.paused")
                        : getString("remainingTime.calculating"));
                return;
            }
            if (unknownPhase) {
                searchRemainingTimeLabel.setText(getString("remainingTime.calculating"));
                return;
            }
            searchRemainingTimeLabel.setText(info.remainingMs() > 0
                    ? getString("remainingTime", formatTime(info.remainingMs()))
                    : getString("remainingTime.calculating"));
        });
    }

    private void enableSearchControls() {
        isSearchRunning = false;
        isSearchPaused = false;
        searchStartButton.setEnabled(true);
        searchPauseButton.setEnabled(false);
        searchPauseButton.setText(getString("button.pause"));
        searchStopButton.setEnabled(false);
        searchResetButton.setEnabled(true);
        if (searchResetToWorldBorderButton != null) searchResetToWorldBorderButton.setEnabled(true);
        searchSeedField.setEnabled(true);
        searchThreadCountField.setEnabled(true);
        if (searchFastModeCheckBox != null) searchFastModeCheckBox.setEnabled(true);
        if (searchMcVersionComboBox != null) searchMcVersionComboBox.setEnabled(true);
        applyFilterEnable(true);
        enableSideFields(true);
        applyBoundFieldEnableState(true);
        if (languageComboBox != null) languageComboBox.setEnabled(true);
    }

    private void addSearchResult(long resultToken, String result) {
        SwingUtilities.invokeLater(() -> {
            if (resultToken != singleSearchResultToken.get()) {
                return;
            }
            if (result == null || result.isEmpty()) {
                return;
            }
            searchResultArea.append(result);
            if (!result.endsWith("\n")) {
                searchResultArea.append("\n");
            }
            searchResultArea.setCaretPosition(searchResultArea.getDocument().getLength());
        });
    }

    private void exportSearchResults() {
        String resultText = searchResultArea.getText();
        if (resultText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("error.noResultsToExport"), getString("prompt.information"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(getString("dialog.exportResults"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(getString("dialog.textFiles"), "txt"));
        fileChooser.setSelectedFile(new File(getString("file.searchOutput")));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // 导出所有结果，包括带有"x"标记的无法生成的结果
                writer.print(resultText);
                JOptionPane.showMessageDialog(this, getString("success.export"), getString("prompt.success"), JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, getString("error.exportFailed", e.getMessage()), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return getString("time.format", days, hours, minutes, seconds);
    }

    /**
     * 设置窗口图标
     * Icon file should be at src/main/resources/icon.png or icon.ico
     */
    private void setWindowIcon() {
        try {
            // 尝试从资源文件加载图标
            java.net.URL iconURL = getClass().getResource("/icon.png");
            if (iconURL == null) {
                iconURL = getClass().getResource("/icon.ico");
            }
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                setIconImage(icon.getImage());
            } else {
                // 如果没有找到图标文件，可以创建一个简单的默认图标
                // 如果没有找到图标文件，可以创建一个简单的默认图标
                System.out.println("提示: 未找到图标文件 (icon.png 或 icon.ico)，使用系统默认图标");
            }
        } catch (Exception e) {
            System.err.println("设置图标时出错: " + e.getMessage());
        }
    }

    /**
     * 设置字体
     * Load font.ttf from resources
     */
    private void setChineseFont() {
        try {
            // 从资源文件加载 font.ttf
            java.io.InputStream fontStream = getClass().getResourceAsStream("/font.ttf");
            if (fontStream == null) {
                System.err.println("错误: 未找到字体文件 font.ttf，请确保文件位于 src/main/resources/font.ttf");
                return;
            }

            // 创建字体
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            fontStream.close();

            // 注册字体到系统
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);

            // 创建指定大小的字体并保存
            loadedFont = font.deriveFont(Font.PLAIN, 12f);

            // 设置全局字体
            UIManager.put("Label.font", loadedFont);
            UIManager.put("Button.font", loadedFont);
            UIManager.put("TextField.font", loadedFont);
            UIManager.put("TextArea.font", loadedFont);
            UIManager.put("ComboBox.font", loadedFont);
            UIManager.put("TabbedPane.font", loadedFont);
            UIManager.put("ProgressBar.font", loadedFont);
            UIManager.put("ToolTip.font", loadedFont);
            UIManager.put("Menu.font", loadedFont);
            UIManager.put("MenuItem.font", loadedFont);
            UIManager.put("CheckBox.font", loadedFont);
            UIManager.put("RadioButton.font", loadedFont);
            UIManager.put("List.font", loadedFont);
            UIManager.put("Table.font", loadedFont);
            UIManager.put("Tree.font", loadedFont);

            System.out.println("成功加载字体: " + font.getFontName() + " (大小: " + loadedFont.getSize() + ")");
        } catch (FontFormatException e) {
            System.err.println("字体文件格式错误: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("读取字体文件时出错: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("加载字体时出错: " + e.getMessage());
        }
    }

    /**
     * Return loaded font, or a default sans-serif if unavailable.
     */
    private Font getLoadedFont() {
        if (loadedFont != null) {
            return loadedFont;
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    /**
     * Switch UI language.
     */
    private void changeLanguage() {
        // 如果正在搜索或暂停，不允许切换语言
        if (isSearchRunning || isSearchPaused || isListSearchRunning || isListSearchPaused) {
            // 恢复原来的选择
            if (languageComboBox != null) {
                if (currentLocale.getLanguage().equals("zh")) {
                    languageComboBox.setSelectedIndex(0);
                } else {
                    languageComboBox.setSelectedIndex(1);
                }
            }
            return;
        }

        // 获取选择的语言（用 index，避免中英文选项文案不一致）
        int selectedIndex = languageComboBox != null ? languageComboBox.getSelectedIndex() : -1;
        if (selectedIndex < 0) {
            return;
        }

        Locale newLocale = (selectedIndex == 0) ? new Locale("zh", "CN") : new Locale("en", "US");

        // 如果语言没有变化，不执行切换
        if (newLocale.equals(currentLocale)) {
            return;
        }

        // 重新加载ResourceBundle
        try {
            currentLocale = newLocale;
            messages = ResourceBundle.getBundle("messages", currentLocale);
        } catch (Exception e) {
            System.err.println("加载语言资源失败: " + e.getMessage());
            return;
        }


        // 更新所有UI文本
        updateUITexts();
    }

    /**
     * 更新??有UI文本
     */
    private void updateUITexts() {
        SwingUtilities.invokeLater(() -> {
            // 更新窗口标题
            setTitle(getString("window.title"));

            // 更新标签页标题
            JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
            if (tabbedPane != null) {
                tabbedPane.setTitleAt(0, getString("tab.singleSeedSearch"));
                tabbedPane.setTitleAt(1, getString("tab.listSearch"));
            }

            // 更新单种子搜索面板的所有文本
            updateSingleSeedSearchTexts();

            // 更新列表搜索面板的所有文本
            updateListSearchTexts();
        });
    }

    /**
     * 更新???子搜索面板的文本
     */
    private void updateSingleSeedSearchTexts() {
        // 更新所有标签文本
        if (searchSeedLabel != null) {
            searchSeedLabel.setText(getString("label.seed"));
        }
        if (searchThreadCountLabel != null) {
            searchThreadCountLabel.setText(getString("label.threadCount"));
        }
        if (searchLongSideLabel != null) {
            searchLongSideLabel.setText(getString("label.longSide"));
        }
        if (searchShortSideCheckBox != null) {
            searchShortSideCheckBox.setText(getString("label.shortSide"));
        }
        if (searchViewDistanceLabel != null) {
            searchViewDistanceLabel.setText(getString("label.viewDistance"));
        }
        if (searchFastModeCheckBox != null) {
            searchFastModeCheckBox.setText(getString("label.fastMode"));
        }
        if (searchBothWildflowersCheckBox != null) {
            searchBothWildflowersCheckBox.setText(getString("label.requireBothWildflowers"));
        }
        if (searchBasaltCheckBox != null) {
            searchBasaltCheckBox.setText(getString("label.requireBasaltDeltas"));
        }
        if (searchOrientationCheckBox != null) {
            searchOrientationCheckBox.setText(getString("label.requireOrientation"));
        }
        refreshOrientationCombo(searchOrientationComboBox);
        if (searchFlowerCenterCheckBox != null) {
            searchFlowerCenterCheckBox.setText(getString("label.flowerCenterMargin"));
            searchFlowerCenterCheckBox.setToolTipText(getString("tooltip.flowerCenterMargin"));
        }
        if (searchFlowerCenterField != null) {
            searchFlowerCenterField.setToolTipText(getString("tooltip.flowerCenterMargin"));
        }
        if (searchMcVersionLabel != null) {
            searchMcVersionLabel.setText(getString("label.mcVersion"));
        }
        if (searchMinXLabel != null) {
            searchMinXLabel.setText(getString("label.minX"));
        }
        if (searchMaxXLabel != null) {
            searchMaxXLabel.setText(getString("label.maxX"));
        }
        if (searchMinZLabel != null) {
            searchMinZLabel.setText(getString("label.minZ"));
        }
        if (searchMaxZLabel != null) {
            searchMaxZLabel.setText(getString("label.maxZ"));
        }
        if (squareSideCheckBox != null) {
            squareSideCheckBox.setText(getString("label.squareSide"));
        }
        if (searchLanguageLabel != null) {
            searchLanguageLabel.setText(getString("label.language"));
        }

        // 更新credit文本
        if (searchCreditLabel != null) {
            searchCreditLabel.setText(buildCreditText());
        }

        // 更新右侧面板边框
        if (searchRightPanel != null) {
            searchRightPanel.setBorder(BorderFactory.createTitledBorder(getString("result.border")));
        }

        // 更新按钮文本
        if (searchStartButton != null) {
            searchStartButton.setText(getString("button.startSearch"));
        }
        if (searchPauseButton != null) {
            searchPauseButton.setText(isSearchPaused ? getString("button.resume") : getString("button.pause"));
        }
        if (searchStopButton != null) {
            searchStopButton.setText(getString("button.stop"));
        }
        if (searchResetButton != null) {
            searchResetButton.setText(getString("button.reset"));
        }
        if (searchResetToWorldBorderButton != null) {
            searchResetToWorldBorderButton.setText(getString("button.resetToWorldBorder"));
        }
        if (searchExportButton != null) {
            searchExportButton.setText(getString("button.export"));
        }
        if (searchSortButton != null) {
            searchSortButton.setText(getString("button.sort"));
        }

        // 更新进度条和标签
        if (searchProgressBar != null && !isSearchRunning) {
            searchProgressBar.setIndeterminate(false);
            searchProgressBar.setString(getString("progress.format", 0, 0, 0.0));
        }
        if (searchElapsedTimeLabel != null && !isSearchRunning) {
            searchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(0)));
        }
        if (searchRemainingTimeLabel != null && !isSearchRunning) {
            searchRemainingTimeLabel.setText(getString("remainingTime.calculating"));
        }
    }

    /**
     * Update seed-list search panel texts.
     */
    private void updateListSearchTexts() {
        // 更新所有标签文本
        if (listSearchSeedFileTitleLabel != null) {
            listSearchSeedFileTitleLabel.setText(getString("label.seedFile"));
        }
        if (listSearchThreadCountLabel != null) {
            listSearchThreadCountLabel.setText(getString("label.threadCount"));
        }
        if (listSearchMinXLabel != null) {
            listSearchMinXLabel.setText(getString("label.minX"));
        }
        if (listSearchMaxXLabel != null) {
            listSearchMaxXLabel.setText(getString("label.maxX"));
        }
        if (listSearchMinZLabel != null) {
            listSearchMinZLabel.setText(getString("label.minZ"));
        }
        if (listSearchMaxZLabel != null) {
            listSearchMaxZLabel.setText(getString("label.maxZ"));
        }
        if (listSquareSideCheckBox != null) {
            listSquareSideCheckBox.setText(getString("label.squareSide"));
        }
        if (listLongSideLabel != null) {
            listLongSideLabel.setText(getString("label.longSide"));
        }
        if (listShortSideCheckBox != null) {
            listShortSideCheckBox.setText(getString("label.shortSide"));
        }
        if (listViewDistanceLabel != null) {
            listViewDistanceLabel.setText(getString("label.viewDistance"));
        }
        if (listFastModeCheckBox != null) {
            listFastModeCheckBox.setText(getString("label.fastMode"));
        }
        if (listBothWildflowersCheckBox != null) {
            listBothWildflowersCheckBox.setText(getString("label.requireBothWildflowers"));
        }
        if (listBasaltCheckBox != null) {
            listBasaltCheckBox.setText(getString("label.requireBasaltDeltas"));
        }
        if (listOrientationCheckBox != null) {
            listOrientationCheckBox.setText(getString("label.requireOrientation"));
        }
        refreshOrientationCombo(listOrientationComboBox);
        if (listFlowerCenterCheckBox != null) {
            listFlowerCenterCheckBox.setText(getString("label.flowerCenterMargin"));
            listFlowerCenterCheckBox.setToolTipText(getString("tooltip.flowerCenterMargin"));
        }
        if (listFlowerCenterField != null) {
            listFlowerCenterField.setToolTipText(getString("tooltip.flowerCenterMargin"));
        }
        if (listMcVersionLabel != null) {
            listMcVersionLabel.setText(getString("label.mcVersion"));
        }

        // 更新按钮文本
        if (listSearchStartButton != null) {
            listSearchStartButton.setText(getString("button.startSearch"));
        }
        if (listSearchPauseButton != null) {
            listSearchPauseButton.setText(isListSearchPaused ? getString("button.resume") : getString("button.pause"));
        }
        if (listSearchStopButton != null) {
            listSearchStopButton.setText(getString("button.stop"));
        }
        if (listSearchResetButton != null) {
            listSearchResetButton.setText(getString("button.resetList"));
        }
        if (listSearchExportButton != null) {
            listSearchExportButton.setText(getString("button.export"));
        }
        if (listSearchExportSeedListButton != null) {
            listSearchExportSeedListButton.setText(getString("button.exportSeedList"));
        }
        if (listSortButton != null) {
            listSortButton.setText(getString("button.sort"));
        }
        if (listSearchSeedFileButton != null) {
            listSearchSeedFileButton.setText(getString("button.selectFile"));
        }

        // 更新种子文件标签
        if (listSearchSeedFileLabel != null) {
            if (selectedSeedFile != null && selectedSeedFile.exists()) {
                listSearchSeedFileLabel.setText(selectedSeedFile.getName());
            } else {
                listSearchSeedFileLabel.setText(getString("label.noFileSelected"));
            }
        }

        // 更新进度条和标签
        if (listSearchProgressBar != null && !isListSearchRunning) {
            listSearchProgressBar.setString(getString("progress.total", 0, 0, 0.0));
        }
        if (listSearchElapsedTimeLabel != null && !isListSearchRunning) {
            listSearchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(0)));
        }
        if (listSearchRemainingTimeLabel != null && !isListSearchRunning) {
            listSearchRemainingTimeLabel.setText(getString("remainingTime.calculating"));
        }
        if (listSearchCurrentSeedProgressLabel != null && !isListSearchRunning) {
            listSearchCurrentSeedProgressLabel.setText(getString("currentSeed.default"));
        }

        // 更新credit文本
        if (listSearchCreditLabel != null) {
            listSearchCreditLabel.setText(buildCreditText());
        }

        // 更新右侧面板边框
        if (listSearchRightPanel != null) {
            listSearchRightPanel.setBorder(BorderFactory.createTitledBorder(getString("result.border")));
        }
    }

    // ========== 从种子列表搜索相关方法 ==========

    // ========== 从种子列表搜索相关方法 ==========
    private void addListSearchParameterListeners() {
        listLongSideField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });
        listShortSideField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });

        // 坐标变化监听
        listMinXField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });
        listMaxXField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });
        listMinZField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });
        listMaxZField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                checkListSearchParameterChange();
            }
        });
    }

    // 检查搜索参数是否变化（除了线程数）
    private void checkListSearchParameterChange() {
        if (isListSearchRunning && !isListSearchPaused) {
            return; // running and not paused
        }

        if (!isListSearchPaused) {
            return; // not paused
        }

        try {
            if (selectedSeedFile == null) return;
            int minArea = readLongSide(listLongSideField);
            if (minArea < 0) minArea = 0;
            int minX = Integer.parseInt(listMinXField.getText().trim());
            int maxX = Integer.parseInt(listMaxXField.getText().trim());
            int minZ = Integer.parseInt(listMinZField.getText().trim());
            int maxZ = Integer.parseInt(listMaxZField.getText().trim());

            if (minX != lastListSearchMinX || maxX != lastListSearchMaxX || minZ != lastListSearchMinZ || maxZ != lastListSearchMaxZ || minArea != lastListSearchMinArea) {
                if (listSearchRunner != null) listSearchRunner.stop();
                isListSearchRunning = false;
                isListSearchPaused = false;
                listSearchStartButton.setEnabled(true);
                listSearchPauseButton.setEnabled(false);
                listSearchPauseButton.setText(getString("button.pause"));
                listSearchStopButton.setEnabled(false);
                listSearchResetButton.setEnabled(true);
                listSearchSeedFileButton.setEnabled(true);
                listSearchThreadCountField.setEnabled(true);
                if (listFastModeCheckBox != null) listFastModeCheckBox.setEnabled(true);
                if (listMcVersionComboBox != null) listMcVersionComboBox.setEnabled(true);
                enableSideFields(false);
                applyListBoundFieldEnableState(true);
                listSearchResultToken.incrementAndGet();
                listSearchResultArea.setText("");
                listSearchProgressBar.setValue(0);
                listSearchProgressBar.setString(getString("progress.total", 0, 0, 0.0));
                listSearchCurrentSeedProgressLabel.setText(getString("currentSeed.default"));
                listSearchRemainingTimeLabel.setText(getString("remainingTime.reset"));
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    // 选择种子文件
    private void selectSeedFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(getString("dialog.selectSeedFile"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(getString("dialog.textFiles"), "txt"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedSeedFile = fileChooser.getSelectedFile();
            listSearchSeedFileLabel.setText(selectedSeedFile.getName());
        }
    }

    // 读取种子列表
    private List<Long> readSeedList(File file) throws IOException {
        List<Long> seeds = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    long seed = Long.parseLong(line);
                    seeds.add(seed);
                } catch (NumberFormatException e) {
                    // 跳过无效的种子行
                    System.err.println("跳过无效的种子行: " + line);
                }
            }
        }
        return seeds;
    }

    // 列表搜索（河流多阶段，多种子顺序执行）
    private void startListSearch() {
        if (isListSearchRunning && isListSearchPaused) {
            if (listSearchRunner != null) {
                listSearchRunner.resume();
                isListSearchPaused = false;
                listSearchPauseButton.setText(getString("button.pause"));
            }
            return;
        }

        try {
            // 验证种子文件
            if (selectedSeedFile == null || !selectedSeedFile.exists()) {
                JOptionPane.showMessageDialog(this, getString("error.seedFileRequired"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 读取种子列表
            List<Long> seeds;
            try {
                seeds = readSeedList(selectedSeedFile);
                if (seeds.isEmpty()) {
                    JOptionPane.showMessageDialog(this, getString("error.seedFileEmpty"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 在导入时设置进度条为 0/种子数
                final long totalSeeds = seeds.size();
                SwingUtilities.invokeLater(() -> {
                    listSearchProgressBar.setMaximum((int) totalSeeds);
                    listSearchProgressBar.setValue(0);
                    listSearchProgressBar.setString(getString("progress.seedsDone", 0, totalSeeds));
                    listSearchCurrentSeedProgressLabel.setText(getString("currentSeed.default"));
                });
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, getString("error.seedFileReadFailed", e.getMessage()), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 验证线程数
            String threadText = listSearchThreadCountField.getText().trim();
            double threadDouble;
            try {
                threadDouble = Double.parseDouble(threadText);
                if (threadDouble != Math.floor(threadDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.threadCountMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.threadCountFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            int threadCount = (int) threadDouble;
            if (threadCount < 1) {
                JOptionPane.showMessageDialog(this, getString("error.threadCountRequired"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查线程数是否超过CPU核数
            int cpuThreads = Runtime.getRuntime().availableProcessors();
            if (threadCount > cpuThreads) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        getString("error.threadCountExceedsCPU", cpuThreads, cpuThreads),
                        getString("prompt.adjustThreadCount"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (result == JOptionPane.YES_OPTION) {
                    threadCount = cpuThreads;
                    listSearchThreadCountField.setText(String.valueOf(cpuThreads));
                } else {
                    return;
                }
            }

            boolean fastMode = isListFastMode();
            boolean bothWildflowers = isBothWildflowersFilter(listBothWildflowersCheckBox);
            boolean basaltDeltas = isBasaltFilter(listBasaltCheckBox);
            int orientation = selectedOrientation(listOrientationCheckBox, listOrientationComboBox);
            String mcVersion = selectedMcVersion(listMcVersionComboBox);
            int longSide = readLongSide(listLongSideField);
            if (longSide < 0) {
                JOptionPane.showMessageDialog(this, getString("error.longSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean squareOnly = listShortSideCheckBox == null || !listShortSideCheckBox.isSelected();
            int shortSide = readShortSide(listLongSideField, listShortSideField, listShortSideCheckBox);
            if (shortSide < 0) {
                JOptionPane.showMessageDialog(this, getString("error.shortSideInvalid"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            int demandSideX = longSide;
            int demandSideZ = squareOnly ? longSide : shortSide;
            int flowerCenterMargin = readFlowerCenterMargin(listFlowerCenterCheckBox, listFlowerCenterField,
                    demandSideX, demandSideZ);
            if (reportFlowerCenterMarginError(flowerCenterMargin, demandSideX, demandSideZ)) {
                return;
            }

            // 验证XZ坐标
            String minXText = listMinXField.getText().trim();
            String maxXText = listMaxXField.getText().trim();
            String minZText = listMinZField.getText().trim();
            String maxZText = listMaxZField.getText().trim();

            // 检查是否为整数
            double minXDouble, maxXDouble, minZDouble, maxZDouble;
            try {
                minXDouble = Double.parseDouble(minXText);
                if (minXDouble != Math.floor(minXDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.minXMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    listMinXField.setText(String.valueOf(rangeMinForSide(defaultListRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.minXFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                listMinXField.setText(String.valueOf(rangeMinForSide(defaultListRangeSide())));
                return;
            }

            try {
                maxXDouble = Double.parseDouble(maxXText);
                if (maxXDouble != Math.floor(maxXDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.maxXMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    listMaxXField.setText(String.valueOf(rangeMaxForSide(defaultListRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.maxXFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                listMaxXField.setText(String.valueOf(rangeMaxForSide(defaultListRangeSide())));
                return;
            }

            try {
                minZDouble = Double.parseDouble(minZText);
                if (minZDouble != Math.floor(minZDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.minZMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    listMinZField.setText(String.valueOf(rangeMinForSide(defaultListRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.minZFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                listMinZField.setText(String.valueOf(rangeMinForSide(defaultListRangeSide())));
                return;
            }

            try {
                maxZDouble = Double.parseDouble(maxZText);
                if (maxZDouble != Math.floor(maxZDouble)) {
                    JOptionPane.showMessageDialog(this, getString("error.maxZMustBeInteger"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                    listMaxZField.setText(String.valueOf(rangeMaxForSide(defaultListRangeSide())));
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, getString("error.maxZFormatError"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                listMaxZField.setText(String.valueOf(rangeMaxForSide(defaultListRangeSide())));
                return;
            }

            int minX = (int) minXDouble;
            int maxX = (int) maxXDouble;
            int minZ = (int) minZDouble;
            int maxZ = (int) maxZDouble;

            if (minX >= maxX) {
                JOptionPane.showMessageDialog(this, getString("error.minXGreaterThanMaxX"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (minZ >= maxZ) {
                JOptionPane.showMessageDialog(this, getString("error.minZGreaterThanMaxZ"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            long listSearchArea = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
            if (listSearchArea > FlowerFarmFinderSearchRunner.MAX_SEARCH_AREA_BLOCKS) {
                JOptionPane.showMessageDialog(this, getString("error.searchAreaTooLarge"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            lastListSearchMinX = minX;
            lastListSearchMaxX = maxX;
            lastListSearchMinZ = minZ;
            lastListSearchMaxZ = maxZ;
            lastListSearchMinArea = longSide;
            lastListSearchThreadCount = threadCount;

            isListSearchRunning = true;
            isListSearchPaused = false;
            listSearchStartButton.setEnabled(false);
            listSearchPauseButton.setEnabled(true);
            listSearchPauseButton.setText(getString("button.pause"));
            listSearchStopButton.setEnabled(true);
            listSearchResetButton.setEnabled(false);
            listSearchSeedFileButton.setEnabled(false);
            listSearchThreadCountField.setEnabled(false);
            listLongSideField.setEnabled(false);
            listShortSideField.setEnabled(false);
            if (listShortSideCheckBox != null) listShortSideCheckBox.setEnabled(false);
            listViewDistanceField.setEnabled(false);
            if (listFastModeCheckBox != null) listFastModeCheckBox.setEnabled(false);
            if (listBothWildflowersCheckBox != null) listBothWildflowersCheckBox.setEnabled(false);
            if (listBasaltCheckBox != null) listBasaltCheckBox.setEnabled(false);
            if (listOrientationCheckBox != null) listOrientationCheckBox.setEnabled(false);
            if (listOrientationComboBox != null) listOrientationComboBox.setEnabled(false);
            if (listFlowerCenterCheckBox != null) listFlowerCenterCheckBox.setEnabled(false);
            if (listFlowerCenterField != null) listFlowerCenterField.setEnabled(false);
            if (listMcVersionComboBox != null) listMcVersionComboBox.setEnabled(false);
            applyListBoundFieldEnableState(false);
            final long listResultToken = listSearchResultToken.incrementAndGet();
            listSearchResultArea.setText("");
            listSearchProgressBar.setMaximum((int) seeds.size());
            listSearchProgressBar.setValue(0);
            listSearchProgressBar.setString(getString("progress.seedsDone", 0, seeds.size()));
            listSearchCurrentSeedProgressLabel.setText(getString("currentSeed.default"));
            listSearchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(0)));
            listSearchRemainingTimeLabel.setText(getString("remainingTime.calculating"));

            seedResults.clear();

            final int finalLongSide = longSide;
            final int finalShortSide = shortSide;
            final boolean finalSquareOnly = squareOnly;
            final int finalThreadCount = threadCount;
            final boolean finalFastMode = fastMode;
            final boolean finalBothWildflowers = bothWildflowers;
            final boolean finalBasaltDeltas = basaltDeltas;
            final int finalOrientation = orientation;
            final int finalFlowerCenterMargin = flowerCenterMargin;
            final String finalMcVersion = mcVersion;
            final long totalSeeds = seeds.size();
            final long startTime = System.currentTimeMillis();
            // 暂停时间跟踪
            final long[] pausedTimeRef = {0}; // 累计暂停时间
            final long[] pauseStartTimeRef = {0}; // 暂停开始时间

            // 启动进度监控线程，定期更新时间显示
            Thread progressMonitorThread = new Thread(() -> {
                while (isListSearchRunning) {
                    try {
                        Thread.sleep(100); // 每100ms更新一次

                        // 更新暂停时间跟踪
                        if (isListSearchPaused) {
                            // 记录暂停开始时间
                            if (pauseStartTimeRef[0] == 0) {
                                pauseStartTimeRef[0] = System.currentTimeMillis();
                            }
                        } else {
                            // 如果从暂停恢复，累计暂停时间
                            if (pauseStartTimeRef[0] > 0) {
                                pausedTimeRef[0] += System.currentTimeMillis() - pauseStartTimeRef[0];
                                pauseStartTimeRef[0] = 0;
                            }
                        }

                        // 计算实际已用时间（排除暂停时间）
                        long currentPausedTime = pausedTimeRef[0];
                        if (pauseStartTimeRef[0] > 0) {
                            // 如果当前正在暂停，也要计入当前暂停时间
                            currentPausedTime += System.currentTimeMillis() - pauseStartTimeRef[0];
                        }
                        final long elapsedMs = System.currentTimeMillis() - startTime - currentPausedTime;

                        // 获取当前完成的种子数（需要从UI获取或使用共享变量）
                        SwingUtilities.invokeLater(() -> {
                            if (listResultToken != listSearchResultToken.get()) {
                                return;
                            }
                            int currentProgress = listSearchProgressBar.getValue();
                            if (currentProgress > 0 && currentProgress < totalSeeds) {
                                if (isListSearchPaused) {
                                    listSearchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(elapsedMs)));
                                    listSearchRemainingTimeLabel.setText(getString("remainingTime.paused"));
                                    return;
                                }
                                final long remainingMs = elapsedMs > 0 ? (elapsedMs * (totalSeeds - currentProgress) / currentProgress) : 0;
                                listSearchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(elapsedMs)));
                                if (remainingMs > 0) {
                                    listSearchRemainingTimeLabel.setText(getString("remainingTime", formatTime(remainingMs)));
                                } else {
                                    listSearchRemainingTimeLabel.setText(getString("remainingTime.calculating"));
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            progressMonitorThread.setDaemon(true);
            progressMonitorThread.start();

            new Thread(() -> {
                final int[] processedSeedsRef = {0};
                final long[] lastProgressUpdate = {0};
                final long PROGRESS_INTERVAL_MS = 100;

                for (int seedIndex = 0; seedIndex < seeds.size(); seedIndex++) {
                    if (!isListSearchRunning) break;

                    final long seed = seeds.get(seedIndex);
                    final int currentSeedIndex = seedIndex + 1;
                    lastProgressUpdate[0] = 0;
                    seedResults.put(seed, new ArrayList<>());

                    listSearchRunner = new FlowerFarmFinderSearchRunner();
                    final long currentSeed = seed;
                    Consumer<String> seedResultCallback = result -> {
                        if (result == null || result.isEmpty()) {
                            return;
                        }
                        List<String> lines = seedResults.get(currentSeed);
                        for (String line : result.split("\n", -1)) {
                            if (!line.isEmpty()) {
                                lines.add(line);
                            }
                        }
                    };
                    Consumer<FlowerFarmFinderSearchRunner.ProgressInfo> seedProgressCallback = info -> {
                        if (info.done()) return;
                        if (listResultToken != listSearchResultToken.get()) return;
                        if (isListSearchPaused && info.nativePauseSettled()) {
                            return;
                        }
                        long now = System.currentTimeMillis();
                        if (now - lastProgressUpdate[0] < PROGRESS_INTERVAL_MS) return;
                        lastProgressUpdate[0] = now;
                        final long proc = info.processed();
                        final long tot = info.total();
                        final double pct = tot > 0 ? Math.min(100.0, proc * 100.0 / tot) : 0;
                        final int phase = info.phase();
                        SwingUtilities.invokeLater(() -> {
                            if (listResultToken != listSearchResultToken.get()) {
                                return;
                            }
                            if (isListSearchRunning) {
                                // Phase 2 reports its own counters (from 0).
                                boolean unknownPhase = (phase == -1);
                                String key = (phase == PHASE_MERGE || unknownPhase) ? "currentSeed.stage2"
                                        : (phase == PHASE_VERIFY) ? "currentSeed.stage3" : "currentSeed.stage1";
                                listSearchCurrentSeedProgressLabel.setText(unknownPhase
                                        ? getString(key, currentSeedIndex, totalSeeds, 0, 0, 0.0)
                                        : getString(key, currentSeedIndex, totalSeeds, proc, tot, pct));
                            }
                        });
                    };

                    if (listShortSideCheckBox != null) listShortSideCheckBox.setEnabled(false);
                    listSearchRunner.runFlowerSearchBlocking(seed, minX, maxX, minZ, maxZ, finalLongSide, finalShortSide, finalSquareOnly, finalThreadCount, finalFastMode, finalBothWildflowers, finalBasaltDeltas, finalOrientation, finalFlowerCenterMargin, finalMcVersion, seedProgressCallback, seedResultCallback);

                    while (listSearchRunner.isRunning() && isListSearchRunning) {
                        while (isListSearchPaused && isListSearchRunning) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        if (!isListSearchRunning) break;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    if (!isListSearchRunning) break;

                    processedSeedsRef[0]++;
                    final int completedSeeds = processedSeedsRef[0];
                    SwingUtilities.invokeLater(() -> {
                        if (listResultToken != listSearchResultToken.get()) {
                            return;
                        }
                        listSearchProgressBar.setValue(completedSeeds);
                        listSearchProgressBar.setString(getString("progress.seedsDone", completedSeeds, totalSeeds));
                    });

                    List<String> results = seedResults.get(seed);
                    if (!results.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            if (listResultToken != listSearchResultToken.get()) {
                                return;
                            }
//                            if (listSearchResultArea.getDocument().getLength() > 0) {
//                                listSearchResultArea.append("\n");
//                            }
                            listSearchResultArea.append(seed + "\n");
                            for (String result : results) {
                                listSearchResultArea.append(result + "\n");
                            }
                            listSearchResultArea.setCaretPosition(listSearchResultArea.getDocument().getLength());
                        });
                    }
                }

                // 若已停止：保持界面停留在“正常显示”的最后一帧，不进入“已完成”状态
                if (!isListSearchRunning) {
                    return;
                }

                // 所有种子处理完成
                // 所有种子处理完成
                long finalPausedTime = pausedTimeRef[0];
                if (pauseStartTimeRef[0] > 0) {
                    // 如果结束时还在暂停，也要计入当前暂停时间
                    finalPausedTime += System.currentTimeMillis() - pauseStartTimeRef[0];
                }
                final long finalElapsedMs = System.currentTimeMillis() - startTime - finalPausedTime;
                SwingUtilities.invokeLater(() -> {
                    if (listResultToken != listSearchResultToken.get()) {
                        return;
                    }
                    listSearchResultToken.incrementAndGet();
                    isListSearchRunning = false;
                    isListSearchPaused = false;
                    listSearchStartButton.setEnabled(true);
                    listSearchPauseButton.setEnabled(false);
                    listSearchPauseButton.setText(getString("button.pause"));
                    listSearchStopButton.setEnabled(false);
                    listSearchResetButton.setEnabled(true);
                    listSearchSeedFileButton.setEnabled(true);
                    listSearchThreadCountField.setEnabled(true);
                    if (listFastModeCheckBox != null) listFastModeCheckBox.setEnabled(true);
                    if (listMcVersionComboBox != null) listMcVersionComboBox.setEnabled(true);
                    applyFilterEnable(false);
                    enableSideFields(false);
                    applyListBoundFieldEnableState(true);
                    listSearchProgressBar.setValue((int) totalSeeds);
                    listSearchProgressBar.setString(getString("progress.seedsDone", totalSeeds, totalSeeds));
                    listSearchCurrentSeedProgressLabel.setText(getString("currentSeed.complete"));
                    listSearchElapsedTimeLabel.setText(getString("elapsedTime", formatTime(finalElapsedMs)));
                    listSearchRemainingTimeLabel.setText(getString("remainingTime.completed"));
                });
            }).start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, getString("error.invalidNumber"), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleListSearchPause() {
        if (listSearchRunner == null || !isListSearchRunning) return;
        if (isListSearchPaused) {
            listSearchRunner.resume();
            isListSearchPaused = false;
            listSearchPauseButton.setText(getString("button.pause"));
        } else {
            listSearchRunner.pause();
            isListSearchPaused = true;
            listSearchPauseButton.setText(getString("button.resume"));
        }
        listSearchThreadCountField.setEnabled(false);
    }

    private void stopListSearch() {
        if (listSearchRunner != null) {
            listSearchRunner.stop();
        }
        listSearchResultToken.incrementAndGet();
        isListSearchRunning = false;
        isListSearchPaused = false;
        listSearchStartButton.setEnabled(true);
        listSearchPauseButton.setEnabled(false);
        listSearchPauseButton.setText(getString("button.pause"));
        listSearchStopButton.setEnabled(false);
        listSearchResetButton.setEnabled(true);
        listSearchSeedFileButton.setEnabled(true);
        listSearchThreadCountField.setEnabled(true);
        if (listFastModeCheckBox != null) listFastModeCheckBox.setEnabled(true);
        if (listMcVersionComboBox != null) listMcVersionComboBox.setEnabled(true);
        applyFilterEnable(false);
        enableSideFields(false);
        applyListBoundFieldEnableState(true);
        listSearchRemainingTimeLabel.setText(getString("remainingTime.stopped"));
    }

    private void resetListSearchToDefaults() {
        applyDefaultListRange();
    }

    private void applyListSearchBounds(int minX, int maxX, int minZ, int maxZ) {
        long side = (long) maxX - minX + 1;
        if (listSquareSideCheckBox != null) {
            listSquareSideCheckBox.setSelected(true);
        }
        if (listSquareSideField != null) {
            listSquareSideUpdating = true;
            listSquareSideField.setText(String.valueOf(side));
            listSquareSideUpdating = false;
        }
        listMinXField.setText(String.valueOf(minX));
        listMaxXField.setText(String.valueOf(maxX));
        listMinZField.setText(String.valueOf(minZ));
        listMaxZField.setText(String.valueOf(maxZ));
        applyListBoundFieldEnableState(!isListSearchRunning);
        applyListSquareSideToBounds();
    }

    /** Parse seed-list result text into seed → /tp hit lines. */
    private Map<Long, List<ListHitResult>> parseListResults() {
        Map<Long, List<ListHitResult>> parsedResults = new HashMap<>();
        String text = listSearchResultArea.getText().trim();
        if (text.isEmpty()) return parsedResults;
        String[] lines = text.split("\n");
        Long currentSeed = null;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                long seed = Long.parseLong(line);
                currentSeed = seed;
                parsedResults.putIfAbsent(seed, new ArrayList<>());
            } catch (NumberFormatException e) {
                if (currentSeed != null && line.startsWith("/tp ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            int x = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[3]);
                            parsedResults.get(currentSeed).add(new ListHitResult(x, z, line));
                        } catch (NumberFormatException ex) { /* skip */ }
                    }
                }
            }
        }
        return parsedResults;
    }

    private static class ListHitResult {
        final int x, z;
        final String originalLine;

        ListHitResult(int x, int z, String originalLine) {
            this.x = x;
            this.z = z;
            this.originalLine = originalLine;
        }

        double distanceSquared() {
            return (double) x * x + (double) z * z;
        }

        boolean hasPinkPetals() {
            return FlowerFarmFinderFrame.hasPinkPetals(originalLine);
        }
    }

    /**
     * Seeds with any cherry result first; then by that seed's nearest hit to origin.
     * Within a seed: cherry first, then nearer to origin.
     */
    private void sortListResults() {
        Map<Long, List<ListHitResult>> parsed = parseListResults();
        if (parsed.isEmpty()) return;
        List<Long> seeds = new ArrayList<>(parsed.keySet());
        seeds.sort((sa, sb) -> {
            List<ListHitResult> ra = parsed.get(sa);
            List<ListHitResult> rb = parsed.get(sb);
            boolean pa = ra.stream().anyMatch(ListHitResult::hasPinkPetals);
            boolean pb = rb.stream().anyMatch(ListHitResult::hasPinkPetals);
            if (pa != pb) {
                return pa ? -1 : 1;
            }
            double da = ra.stream().mapToDouble(ListHitResult::distanceSquared).min().orElse(Double.MAX_VALUE);
            double db = rb.stream().mapToDouble(ListHitResult::distanceSquared).min().orElse(Double.MAX_VALUE);
            int cmp = Double.compare(da, db);
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(sa, sb);
        });
        StringBuilder sb = new StringBuilder();
        for (Long seed : seeds) {
            sb.append(seed).append("\n");
            List<ListHitResult> rows = parsed.get(seed);
            rows.sort((a, b) -> {
                boolean pa = a.hasPinkPetals();
                boolean pb = b.hasPinkPetals();
                if (pa != pb) {
                    return pa ? -1 : 1;
                }
                return Double.compare(a.distanceSquared(), b.distanceSquared());
            });
            for (ListHitResult r : rows) {
                sb.append(r.originalLine).append("\n");
            }
        }
        listSearchResultArea.setText(sb.toString());
    }

    private void exportListSearchResults() {
        if (listSearchResultArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("error.noResultsToExport"), getString("prompt.information"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(getString("dialog.exportResults"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(getString("dialog.textFiles"), "txt"));
        fileChooser.setSelectedFile(new File(getString("file.searchOutput")));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.print(listSearchResultArea.getText());
                JOptionPane.showMessageDialog(this, getString("success.export"), getString("prompt.success"), JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, getString("error.exportFailed", e.getMessage()), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导出种子列表（不含/tp坐标）
    private void exportSeedList() {
        if (listSearchResultArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("error.noResultsToExport"), getString("prompt.information"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 解析结果，提取所有种子
        String text = listSearchResultArea.getText().trim();
        String[] lines = text.split("\n");
        List<Long> seeds = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            // 跳过/tp开头的坐标行
            if (line.startsWith("/tp ")) {
                continue;
            }
            // 尝试解析为种子
            try {
                long seed = Long.parseLong(line);
                if (!seeds.contains(seed)) {
                    seeds.add(seed);
                }
            } catch (NumberFormatException e) {
                // 忽略无效行
            }
        }

        if (seeds.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("error.noSeedsFound"), getString("prompt.information"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(getString("dialog.exportSeedList"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(getString("dialog.textFiles"), "txt"));
        fileChooser.setSelectedFile(new File(getString("file.seedList")));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (Long seed : seeds) {
                    writer.println(seed);
                }
                JOptionPane.showMessageDialog(this, getString("success.exportSeeds", seeds.size()), getString("prompt.success"), JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, getString("error.exportFailed", e.getMessage()), getString("prompt.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Content panel for the settings scroll pane.
     *
     * Tracking the viewport width lets the HTML hint label wrap instead of
     * widening the column, while the natural height is kept so the vertical
     * scrollbar appears when the window is shorter than the settings block.
     */
    private static final class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FlowerFarmFinderFrame().setVisible(true);
        });
    }
}

