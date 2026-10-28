package com.pxy.depositbook.data.model

/**
 * 内置中国境内银行清单（完全离线，不联网抓取）。
 * - 用户从下拉中选择，可用关键字筛选
 * - 显示银行图标；用户自行添加的银行不显示图标
 */
object BankCatalog {

    /** 内置银行（按类型排序：国有大行 → 股份制 → 民营银行 → 城商行 → 农商行 → 外资/外资在华） */
    val BANKS: List<String> = listOf(
        // 国有大型商业银行
        "中国工商银行", "中国农业银行", "中国银行", "中国建设银行", "交通银行", "中国邮政储蓄银行",
        // 全国性股份制
        "招商银行", "中信银行", "上海浦东发展银行", "兴业银行", "中国民生银行", "中国光大银行",
        "平安银行", "华夏银行", "广发银行", "浙商银行", "渤海银行", "恒丰银行",
        // 民营银行（19 家）
        "深圳前海微众银行", "浙江网商银行", "天津金城银行", "上海华瑞银行", "温州民商银行",
        "湖南三湘银行", "重庆富民银行", "四川新网银行", "吉林亿联银行", "武汉众邦银行",
        "北京中关村银行", "江苏苏宁银行", "梅州客商银行", "安徽新安银行", "福建华通银行",
        "威海蓝海银行", "无锡锡商银行", "江西裕民银行", "辽宁振兴银行",
        // 主要城商行
        "北京银行", "上海银行", "江苏银行", "南京银行", "宁波银行", "杭州银行", "苏州银行",
        "青岛银行", "齐鲁银行", "天津银行", "大连银行", "盛京银行", "哈尔滨银行", "吉林银行",
        "锦州银行", "抚顺银行", "汉口银行", "湖北银行", "华融湘江银行", "长沙银行", "广州银行",
        "东莞银行", "珠海华润银行", "厦门银行", "福建海峡银行", "泉州银行", "郑州银行",
        "中原银行", "洛阳银行", "平顶山银行", "长沙银行", "成都银行", "重庆银行", "重庆三峡银行",
        "四川银行", "贵阳银行", "贵州银行", "富滇银行", "兰州银行", "甘肃银行", "西安银行",
        "青海银行", "宁夏银行", "石嘴山银行", "乌鲁木齐银行", "新疆银行", "昆仑银行",
        "内蒙古银行", "蒙商银行", "张家口银行", "河北银行", "廊坊银行",
        // 主要农商行
        "重庆农村商业银行", "北京农商银行", "上海农商银行", "深圳农村商业银行",
        "广州农村商业银行", "成都农村商业银行",
        // 外资在华
        "汇丰银行（中国）", "渣打银行（中国）", "花旗银行（中国）", "东亚银行（中国）",
        "恒生银行（中国）", "星展银行（中国）", "华侨银行（中国）", "大华银行（中国）",
        "南洋商业银行（中国）", "富邦华一银行", "瑞穗银行（中国）", "三菱日联银行（中国）",
        "三井住友银行（中国）", "友利银行（中国）", "新韩银行（中国）", "韩亚银行（中国）",
    )

    /** 常用简称 / 别名 → 内置全称 */
    val ALIASES: Map<String, String> = mapOf(
        "工行" to "中国工商银行", "工商银行" to "中国工商银行",
        "农行" to "中国农业银行", "农业银行" to "中国农业银行",
        "中行" to "中国银行",
        "建行" to "中国建设银行",
        "交行" to "交通银行",
        "邮储" to "中国邮政储蓄银行", "邮储银行" to "中国邮政储蓄银行", "邮政银行" to "中国邮政储蓄银行",
        "招行" to "招商银行", "招商" to "招商银行",
        "中信" to "中信银行", "中信银行" to "中信银行",
        "浦发" to "上海浦东发展银行", "浦东发展" to "上海浦东发展银行",
        "兴业" to "兴业银行",
        "民生" to "中国民生银行",
        "光大" to "中国光大银行",
        "平安" to "平安银行",
        "华夏" to "华夏银行",
        "广发" to "广发银行",
        "浙商" to "浙商银行",
        "渤海" to "渤海银行",
        "恒丰" to "恒丰银行",
        "微众" to "深圳前海微众银行",
        "网商" to "浙江网商银行",
        "金城" to "天津金城银行",
        "华瑞" to "上海华瑞银行",
        "民商" to "温州民商银行",
        "三湘" to "湖南三湘银行",
        "富民" to "重庆富民银行",
        "新网" to "四川新网银行",
        "亿联" to "吉林亿联银行",
        "众邦" to "武汉众邦银行",
        "中关村" to "北京中关村银行",
        "苏宁" to "江苏苏宁银行",
        "客商" to "梅州客商银行",
        "新安" to "安徽新安银行",
        "华通" to "福建华通银行",
        "蓝海" to "威海蓝海银行",
        "锡商" to "无锡锡商银行",
        "裕民" to "江西裕民银行",
        "振兴" to "辽宁振兴银行",
        "新韩" to "新韩银行（中国）",
        "汇丰" to "汇丰银行（中国）",
        "渣打" to "渣打银行（中国）",
        "花旗" to "花旗银行（中国）",
        "东亚" to "东亚银行（中国）",
        "恒生" to "恒生银行（中国）",
        "星展" to "星展银行（中国）",
    )

    /** 银行图标显示字符：优先取别名的首个字，否则按去前缀规则取 */
    private val ICON_CHAR: Map<String, String> = ALIASES.entries.associate { (alias, full) ->
        full to alias.first().toString()
    }

    /** 判断是否内置银行 */
    fun isBuiltIn(name: String): Boolean =
        BANKS.contains(name) || ALIASES.containsValue(name)

    /**
     * 关键字搜索：匹配全称包含，或别名匹配。
     * 返回命中列表（内置 + 别名指向的全称），按名称排序去重。
     */
    fun search(keyword: String): List<String> {
        val kw = keyword.trim()
        if (kw.isEmpty()) return BANKS
        val hits = LinkedHashSet<String>()
        // 别名命中：直接给出指向的全称并置顶
        ALIASES.forEach { (alias, full) ->
            if (alias.contains(kw) || full.contains(kw)) hits.add(full)
        }
        BANKS.forEach { b ->
            if (b.contains(kw)) hits.add(b)
        }
        return hits.toList()
    }

    /** 图标显示字符（如「工」「招」「微」） */
    fun iconChar(bankName: String): String {
        ICON_CHAR[bankName]?.let { return it }
        val stripped = stripPrefix(bankName)
        return stripped.firstOrNull { it.code >= 0x3400 }?.toString() ?: bankName.take(1)
    }

    private val PREFIXES = listOf(
        "深圳前海", "深圳", "上海浦东", "上海", "北京", "浙江", "天津", "武汉", "吉林", "四川",
        "湖南", "重庆", "江苏", "梅州", "安徽", "福建", "威海", "无锡", "江西", "辽宁",
        "广州", "苏州", "杭州", "南京", "宁波", "青岛", "大连", "哈尔滨", "锦州", "抚顺",
        "成都", "贵阳", "贵州", "兰州", "甘肃", "西安", "青海", "宁夏", "石嘴山", "乌鲁木齐",
        "内蒙古", "张家口", "廊坊", "东莞", "厦门", "泉州", "郑州", "洛阳", "平顶山",
        "珠海", "汉口", "石家庄", "昆明",
    )

    /** 去掉地域/机构前缀，返回核心名（如「中国工商银行」→「工商银行」） */
    private fun stripPrefix(name: String): String {
        var s = name.removeSuffix("银行")
        if (s.startsWith("中国") && s.length > 2) s = s.substring(2)
        PREFIXES.forEach { p ->
            if (s.startsWith(p) && s.length > p.length) s = s.substring(p.length)
        }
        return s.ifBlank { name }
    }

    /** 图标核心名（去前缀），如「工商银行」 */
    fun coreName(bankName: String): String = if (ALIASES.containsKey(bankName)) bankName else stripPrefix(bankName).removeSuffix("银行")

    /**
     * 内置银行 → assets/bank_logos 里的矢量 logo 文件名（iconfont 风格彩色图标，本地离线）。
     * 没有收录的银行（多为 2015 年后的民营银行）回退为彩色文字圆标。
     */
    val LOGO_FILES: Map<String, String> = mapOf(
        "北京农商银行" to "beijingnongshangyinhang.group.svg",
        "北京银行" to "beijingyinhang.group.svg",
        "渤海银行" to "bohaiyinhang.group.svg",
        "长沙银行" to "changshayinhang.group.svg",
        "成都银行" to "chengduyinhang.group.svg",
        "重庆农村商业银行" to "chongqingnongshangyinhang.group.svg",
        "重庆三峡银行" to "chongqingsanxiayinhang.group.svg",
        "重庆银行" to "chongqingyinhang.group.svg",
        "大华银行（中国）" to "dahuayinhang.group.svg",
        "大连银行" to "dalianyinhang.group.svg",
        "东莞银行" to "dongguanyinhang.group.svg",
        "三菱日联银行（中国）" to "dongjingsanlingyinhang.group.svg",
        "东亚银行（中国）" to "dongyayinhang.group.svg",
        "富邦华一银行" to "fubanghuayiyinhang.group.svg",
        "富滇银行" to "fudianyinhang.group.svg",
        "福建海峡银行" to "fujianhaixiayinhang.group.svg",
        "抚顺银行" to "fushunyinhang.group.svg",
        "甘肃银行" to "gansuyinhang.group.svg",
        "广发银行" to "guangfayinhang.group.svg",
        "广州银行" to "guangzhouyinhang.group.svg",
        "贵阳银行" to "guiyangyinhang.group.svg",
        "贵州银行" to "guizhouyinhang.group.svg",
        "哈尔滨银行" to "haerbinyinhang.group.svg",
        "友利银行（中国）" to "hanguoyouliyinhang.group.svg",
        "杭州银行" to "hangzhouyinhang.group.svg",
        "汉口银行" to "hankouyinhang.group.svg",
        "韩亚银行（中国）" to "hanyayinhang.group.svg",
        "河北银行" to "hebeiyinhang.group.svg",
        "恒丰银行" to "hengfengyinhang.group.svg",
        "恒生银行（中国）" to "hengshengyinhang.group.svg",
        "华侨银行（中国）" to "huaqiaoyinhang.group.svg",
        "花旗银行（中国）" to "huaqiyinhang.group.svg",
        "华融湘江银行" to "huarongxiangjiangyinhang.group.svg",
        "珠海华润银行" to "huarunyinhang.group.svg",
        "华夏银行" to "huaxiayinhang.group.svg",
        "湖北银行" to "hubeiyinhang.group.svg",
        "汇丰银行（中国）" to "huifengyinhang.group.svg",
        "江苏银行" to "jiangsuyinhang.group.svg",
        "中国建设银行" to "jiansheyinhang.group.svg",
        "交通银行" to "jiaotongyinhang.group.svg",
        "吉林银行" to "jilinyinhang.group.svg",
        "天津金城银行" to "jinchengyinhang.group.svg",
        "锦州银行" to "jinzhouyinhang.group.svg",
        "昆仑银行" to "kunlunyinhang.group.svg",
        "廊坊银行" to "langfangyinhang.group.svg",
        "兰州银行" to "lanzhouyinhang.group.svg",
        "洛阳银行" to "luoyangyinhang.group.svg",
        "南京银行" to "nanjingyinhang.group.svg",
        "南洋商业银行（中国）" to "nanyangshangyeyinhang.group.svg",
        "宁波银行" to "ningboyinhang.group.svg",
        "宁夏银行" to "ningxiayinhang.group.svg",
        "平安银行" to "pinganyinhang.group.svg",
        "平顶山银行" to "pingdingshanyinhang.group.svg",
        "上海浦东发展银行" to "pufayinhang.group.svg",
        "齐鲁银行" to "qiluyinhang.group.svg",
        "青岛银行" to "qingdaoyinhang.group.svg",
        "青海银行" to "qinghaiyinhang.group.svg",
        "泉州银行" to "quanzhouyinhang.group.svg",
        "湖南三湘银行" to "sanxiangyinhang.group.svg",
        "上海农商银行" to "shanghainongshangyinhang.group.svg",
        "上海银行" to "shanghaiyinhang.group.svg",
        "盛京银行" to "shengjingyinhang.group.svg",
        "深圳农村商业银行" to "shenzhennongshangyinhang.group.svg",
        "石嘴山银行" to "shizuishanyinhang.group.svg",
        "苏州银行" to "suzhouyinhang.group.svg",
        "天津银行" to "tianjinyinhang.group.svg",
        "武汉众邦银行" to "wuhanzhongbangyinhang.group.svg",
        "乌鲁木齐银行" to "wulumuqishangyeyinhang.group.svg",
        "厦门银行" to "xiamenyinhang.group.svg",
        "西安银行" to "xianyinhang.group.svg",
        "兴业银行" to "xingyeyinhang.group.svg",
        "星展银行（中国）" to "xingzhanyinhang.group.svg",
        "新韩银行（中国）" to "xinhanyinhang.group.svg",
        "渣打银行（中国）" to "zhadayinhang.group.svg",
        "张家口银行" to "zhangjiakouyinhang.group.svg",
        "招商银行" to "zhaoshangyinhang.group.svg",
        "浙商银行" to "zheshangyinhang.group.svg",
        "中国工商银行" to "zhongguogongshangyinhang.group.svg",
        "中国光大银行" to "zhongguoguangdayinhang.group.svg",
        "中国民生银行" to "zhongguominshengyinhang.group.svg",
        "中国农业银行" to "zhongguonongyeyinhang.group.svg",
        "中国银行" to "zhongguoyinhang.group.svg",
        "中国邮政储蓄银行" to "zhongguoyouzhengchuxuyinhang.group.svg",
        "中信银行" to "zhongxinyinhang.group.svg",
        "中原银行" to "zhongyuanyinhang.group.svg",
    )

    /** 返回银行的真实 logo 资源名；无收录返回 null（外层回退到文字圆标） */
    fun logoFile(bankName: String): String? = LOGO_FILES[bankName]

}
