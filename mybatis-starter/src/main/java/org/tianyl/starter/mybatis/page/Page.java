package org.tianyl.starter.mybatis.page;

/**
 * 后台mybatis插件使用
 * 
 * @author tianyale 2016年9月6日
 *
 */
public class Page {

	public static final int DEFAUT_PAGESIZE = 10;

	private Integer pageNo = 1;// 当前页码
	private Integer pageSize = DEFAUT_PAGESIZE;// 页面大小
	private Long totalCount = 0L;
	private boolean autoCount = true;// 是否自动计算总数

	public Page(int pageNo, int pageSize) {
		this.pageNo = pageNo;
		this.pageSize = pageSize;
	}

	public Page(int pageSize) {
		this.pageSize = pageSize;
	}

	public Page() {

	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Long totalCount) {
		this.totalCount = totalCount;
	}

	public boolean isAutoCount() {
		return autoCount;
	}

	public void setAutoCount(boolean autoCount) {
		this.autoCount = autoCount;
	}

}
