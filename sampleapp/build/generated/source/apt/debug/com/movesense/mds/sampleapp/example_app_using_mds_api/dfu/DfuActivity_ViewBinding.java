// Generated code from Butter Knife. Do not modify!
package com.movesense.mds.sampleapp.example_app_using_mds_api.dfu;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.movesense.mds.sampleapp.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DfuActivity_ViewBinding implements Unbinder {
  private DfuActivity target;

  private View view2131230789;

  private View view2131230803;

  private View view2131230788;

  private View view2131230775;

  @UiThread
  public DfuActivity_ViewBinding(DfuActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public DfuActivity_ViewBinding(final DfuActivity target, View source) {
    this.target = target;

    View view;
    target.dfuApplicationTitleTv = Utils.findRequiredViewAsType(source, R.id.dfu_application_title_tv, "field 'dfuApplicationTitleTv'", TextView.class);
    target.dfuFileNameTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_name_tv, "field 'dfuFileNameTv'", TextView.class);
    target.dfuFileTypeTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_type_tv, "field 'dfuFileTypeTv'", TextView.class);
    target.dfuFileSizeTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_size_tv, "field 'dfuFileSizeTv'", TextView.class);
    target.dfuFileStatusTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_status_tv, "field 'dfuFileStatusTv'", TextView.class);
    target.dfuFileNameValueTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_name_value_tv, "field 'dfuFileNameValueTv'", TextView.class);
    target.dfuFileTypeValueTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_type_value_tv, "field 'dfuFileTypeValueTv'", TextView.class);
    target.dfuFileSizeValueTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_size_value_tv, "field 'dfuFileSizeValueTv'", TextView.class);
    target.dfuFileStatusValueTv = Utils.findRequiredViewAsType(source, R.id.dfu_file_status_value_tv, "field 'dfuFileStatusValueTv'", TextView.class);
    view = Utils.findRequiredView(source, R.id.dfu_select_file_btn, "field 'dfuSelectFileBtn' and method 'onViewClicked'");
    target.dfuSelectFileBtn = Utils.castView(view, R.id.dfu_select_file_btn, "field 'dfuSelectFileBtn'", Button.class);
    view2131230789 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onViewClicked(p0);
      }
    });
    target.dfuDeviceFirmwareUpdateTv = Utils.findRequiredViewAsType(source, R.id.dfu_device_firmware_update_tv, "field 'dfuDeviceFirmwareUpdateTv'", TextView.class);
    view = Utils.findRequiredView(source, R.id.dfu_upload_btn, "field 'dfuUploadBtn' and method 'onViewClicked'");
    target.dfuUploadBtn = Utils.castView(view, R.id.dfu_upload_btn, "field 'dfuUploadBtn'", Button.class);
    view2131230803 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onViewClicked(p0);
      }
    });
    view = Utils.findRequiredView(source, R.id.dfu_select_device_btn, "field 'dfuSelectDeviceBtn' and method 'onViewClicked'");
    target.dfuSelectDeviceBtn = Utils.castView(view, R.id.dfu_select_device_btn, "field 'dfuSelectDeviceBtn'", Button.class);
    view2131230788 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onViewClicked(p0);
      }
    });
    target.parentLayout = Utils.findRequiredViewAsType(source, R.id.parentLayout, "field 'parentLayout'", ConstraintLayout.class);
    target.dfuUploadingTv = Utils.findRequiredViewAsType(source, R.id.dfu_uploading_tv, "field 'dfuUploadingTv'", TextView.class);
    target.dfuUploadingPercentTv = Utils.findRequiredViewAsType(source, R.id.dfu_uploading_percent_tv, "field 'dfuUploadingPercentTv'", TextView.class);
    target.dfuStatusSectionTv = Utils.findRequiredViewAsType(source, R.id.dfu_status_section_tv, "field 'dfuStatusSectionTv'", TextView.class);
    target.dfuStatusTv = Utils.findRequiredViewAsType(source, R.id.dfu_status_tv, "field 'dfuStatusTv'", TextView.class);
    view = Utils.findRequiredView(source, R.id.dfu_enable_dfu_btn, "field 'dfuEnableDfuBtn' and method 'onViewClicked'");
    target.dfuEnableDfuBtn = Utils.castView(view, R.id.dfu_enable_dfu_btn, "field 'dfuEnableDfuBtn'", Button.class);
    view2131230775 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onViewClicked(p0);
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    DfuActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.dfuApplicationTitleTv = null;
    target.dfuFileNameTv = null;
    target.dfuFileTypeTv = null;
    target.dfuFileSizeTv = null;
    target.dfuFileStatusTv = null;
    target.dfuFileNameValueTv = null;
    target.dfuFileTypeValueTv = null;
    target.dfuFileSizeValueTv = null;
    target.dfuFileStatusValueTv = null;
    target.dfuSelectFileBtn = null;
    target.dfuDeviceFirmwareUpdateTv = null;
    target.dfuUploadBtn = null;
    target.dfuSelectDeviceBtn = null;
    target.parentLayout = null;
    target.dfuUploadingTv = null;
    target.dfuUploadingPercentTv = null;
    target.dfuStatusSectionTv = null;
    target.dfuStatusTv = null;
    target.dfuEnableDfuBtn = null;

    view2131230789.setOnClickListener(null);
    view2131230789 = null;
    view2131230803.setOnClickListener(null);
    view2131230803 = null;
    view2131230788.setOnClickListener(null);
    view2131230788 = null;
    view2131230775.setOnClickListener(null);
    view2131230775 = null;
  }
}
