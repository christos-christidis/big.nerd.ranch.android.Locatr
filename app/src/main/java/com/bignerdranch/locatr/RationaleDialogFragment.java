package com.bignerdranch.locatr;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.Objects;

public class RationaleDialogFragment extends DialogFragment {

    interface Callbacks {
        void userClickedOk();
    }

    private Callbacks mCallbacks;

    void setCallbacks(Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    static RationaleDialogFragment newInstance() {
        return new RationaleDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                .setView(R.layout.rationale_dialog_fragment)
                .setTitle(R.string.rationale_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mCallbacks != null) {
                            mCallbacks.userClickedOk();
                            mCallbacks = null;
                        }
                    }
                })
                .create();
    }
}
