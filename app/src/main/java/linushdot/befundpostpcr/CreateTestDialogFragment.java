package linushdot.befundpostpcr;

import android.Manifest;
import android.accounts.Account;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateTestDialogFragment extends DialogFragment {

    private final TestDao testDao;

    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private String barcodeData;

    public CreateTestDialogFragment(TestDao testDao) {
        this.testDao = testDao;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.create_test, null);
        final EditText date = (EditText) view.findViewById(R.id.date);
        date.setText(getDateFormat().format(new Date()));
        final TextView touchToActivate = (TextView) view.findViewById(R.id.touchToActivate);
        final EditText code = (EditText) view.findViewById(R.id.code);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC,     100);
        surfaceView = view.findViewById(R.id.surface);
        barcodeText = code;

        touchToActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(surfaceView.getVisibility() == View.GONE &&
                        touchToActivate.getVisibility() == View.VISIBLE) {
                    touchToActivate.setVisibility(View.GONE);
                    surfaceView.setVisibility(View.VISIBLE);
                    initBarcodeReader();
                }
            }
        });
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(surfaceView.getVisibility() == View.VISIBLE &&
                        touchToActivate.getVisibility() == View.GONE) {
                    if (cameraSource != null) {
                        cameraSource.release();
                    }
                    touchToActivate.setVisibility(View.VISIBLE);
                    surfaceView.setVisibility(View.GONE);
                }
            }
        });

        return builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            final Account account = new Account(
                                    getString(R.string.account_name), getString(R.string.account_type));
                            new MainActivity.CreateTask(testDao,
                                    getDateFormat().parse(date.getText().toString()),
                                    code.getText().toString(),
                                    getString(R.string.content_authority),
                                    account).execute();
                        } catch(ParseException e) {
                            // ignore
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CreateTestDialogFragment.this.getDialog().cancel();
                    }
                })
                .create();
    }

    protected DateFormat getDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    @Override
    public void onPause() {
        super.onPause();
        if(cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getDialog() != null) {
            initBarcodeReader();
        }
    }

    private void initBarcodeReader() {
        if(getContext() == null) {
            return;
        }
        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setRequestedPreviewSize(1280, 720)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if(getActivity() == null) {
                    return;
                }
                try {
                    if(ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if(barcodes.size() != 0) {
                    barcodeText.post(new Runnable() {
                        @Override
                        public void run() {
                            if(barcodes.valueAt(0).email != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).email.address;
                            } else {
                                barcodeData = barcodes.valueAt(0).displayValue;
                            }
                            barcodeText.setText(barcodeData);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(getActivity() == null) {
            return;
        }
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            try {
                if(ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(surfaceView.getHolder());
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
