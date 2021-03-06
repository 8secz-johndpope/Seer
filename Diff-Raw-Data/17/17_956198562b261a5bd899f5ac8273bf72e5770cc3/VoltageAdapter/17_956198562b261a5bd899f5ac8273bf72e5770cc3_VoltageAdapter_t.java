 package rs.pedjaapps.KernelTuner;
 
 
 import java.util.List;
 
 
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.text.InputType;
 import android.view.Gravity;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ImageView;
 import android.widget.ProgressBar;
 import android.widget.SeekBar;
 import android.widget.Toast;
 import android.widget.SeekBar.OnSeekBarChangeListener;
 import android.widget.TextView;
import java.io.File;
 public final class VoltageAdapter extends ArrayAdapter<VoltageEntry> {
 
 	static ProgressDialog pd = null;
 	private final int voltageItemLayoutResource;
 
 	public VoltageAdapter(final Context context, final int voltageItemLayoutResource) {
 	super(context, 0);
 	this.voltageItemLayoutResource = voltageItemLayoutResource;
 	}
 
 	@Override
 	public View getView(final int position, final View convertView, final ViewGroup parent) {
 
 	final View view = getWorkingView(convertView);
 	final ViewHolder viewHolder = getViewHolder(view);
 	final VoltageEntry entry = getItem(position);
 	final List<Integer> allVoltages = CPUInfo.allVoltages();
 	final List<String> voltageFreqs = CPUInfo.voltageFreqs();
 	final List<Integer> voltages = CPUInfo.voltages();
 	
 	viewHolder.voltageSeekBarView.setMax(56);
 	viewHolder.voltageSeekBarView.setProgress(entry.getVoltage());
 	
 	viewHolder.voltageSeekBarView.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
 
 		
 
 		
 		int prog;
 		@Override
 		public void onProgressChanged(SeekBar seekBar, int progress,
 				boolean fromUser) {
 			prog=progress;
 			viewHolder.buttonView.setText(String.valueOf(allVoltages.get(progress)/1000)+"mV");
 			
 		}
 
 		@Override
 		public void onStartTrackingTouch(SeekBar seekBar) {			
 		}
 
 		@Override
 		public void onStopTrackingTouch(SeekBar seekBar) {
 			
 			VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, "Please wait...\nChanging voltage...", true, false);
 			new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", String.valueOf(allVoltages.get(prog)), voltageFreqs.get(position)});
 		}
 		
 	});
 	
 	viewHolder.buttonView.setText(String.valueOf(allVoltages.get(entry.getVoltage())/1000)+"mV");
 	
 	viewHolder.buttonView.setOnClickListener(new OnClickListener(){
 
 		@Override
 		public void onClick(View v) {
 			
 			AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
 			builder.setTitle(voltageFreqs.get(position).substring(0, voltageFreqs.get(position).length()-3)+"Mhz");
 			builder.setMessage("Set new value: ");
 			builder.setIcon(R.drawable.icon);
 
 			 final EditText input = new EditText(view.getContext());              
 			 input.setHint(voltages.get(position).toString());
 			input.setInputType(InputType.TYPE_CLASS_NUMBER);
 			input.setGravity(Gravity.CENTER_HORIZONTAL);
 		
 			builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
					
 					if(!input.getText().toString().equals("")){
						if(new File(CPUInfo.VOLTAGE_PATH).exists()){
 						if(Integer.parseInt(input.getText().toString())>=700000 && Integer.parseInt(input.getText().toString())<=1400000){
 							VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, "Please wait...\nChanging voltage...", true, false);
 							new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", input.getText().toString(), voltageFreqs.get(position)});
 						}
 						else{
 							Toast.makeText(VoltageAdapter.this.getContext(), "Value must be between 700000 and 1400000", Toast.LENGTH_LONG).show();
 						}
						}
						else if(new File(CPUInfo.VOLTAGE_PATH_TEGRA_3).exists()){
							if(Integer.parseInt(input.getText().toString())>=700 && Integer.parseInt(input.getText().toString())<=1400){
								VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, "Please wait...\nChanging voltage...", true, false);
								new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleseek", input.getText().toString(), voltageFreqs.get(position)});
							}
							else{
								Toast.makeText(VoltageAdapter.this.getContext(), "Value must be between 700 and 1400", Toast.LENGTH_LONG).show();
							}
							}
 					}
 					else{
 						Toast.makeText(VoltageAdapter.this.getContext(), "Value can't be empty", Toast.LENGTH_LONG).show();
 						
 					}
 				}
 				
 				
 			});
 			
 			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
 
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					
 				}});
            builder.setView(input);
 			
 			AlertDialog alert = builder.create();
 
 			alert.show();
 			
 		}
 		
 	});
 	
 	viewHolder.minusView.setOnClickListener(new OnClickListener(){
 
 		@Override
 		public void onClick(View v) {
 			VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, "Please wait...\nChanging voltage...", true, false);
 			new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleminus", String.valueOf(position)});
 			
 		}
 		
 	});
 	
 	viewHolder.plusView.setOnClickListener(new OnClickListener(){
 
 		@Override
 		public void onClick(View v) {
 			VoltageAdapter.pd = ProgressDialog.show(VoltageAdapter.this.getContext(), null, "Please wait...\nChanging voltage...", true, false);
 			new ChangeVoltage(VoltageAdapter.this.getContext()).execute(new String[] {"singleplus", String.valueOf(position)});
 			
 		}
 		
 	});
 	
 	viewHolder.freqView.setText(entry.getFreq());
 	/*viewHolder.percentView.setText(entry.getPercent());
 	viewHolder.progressView.setProgress(entry.getProgress());*/
 
 	return view;
 	}
 
 	private View getWorkingView(final View convertView) {
 	View workingView = null;
 
 	if(null == convertView) {
 	final Context context = getContext();
 	final LayoutInflater inflater = (LayoutInflater)context.getSystemService
 	(Context.LAYOUT_INFLATER_SERVICE);
 
 	workingView = inflater.inflate(voltageItemLayoutResource, null);
 	} else {
 	workingView = convertView;
 	}
 
 	return workingView;
 	}
 
 	private ViewHolder getViewHolder(final View workingView) {
 	final Object tag = workingView.getTag();
 	ViewHolder viewHolder = null;
 
 
 	if(null == tag || !(tag instanceof ViewHolder)) {
 	viewHolder = new ViewHolder();
 
 	viewHolder.voltageSeekBarView = (SeekBar) workingView.findViewById(R.id.seekBar1);
 	viewHolder.minusView = (ImageView) workingView.findViewById(R.id.minus);
 	viewHolder.plusView = (ImageView) workingView.findViewById(R.id.plus);
 	viewHolder.buttonView = (Button) workingView.findViewById(R.id.button1);
 	viewHolder.freqView = (TextView)workingView.findViewById(R.id.textView1);
 	
 	workingView.setTag(viewHolder);
 
 	} else {
 	viewHolder = (ViewHolder) tag;
 	}
 
 	return viewHolder;
 	}
 
 	private class ViewHolder {
 	public SeekBar voltageSeekBarView;
 	public ImageView minusView;
 	public ImageView plusView;
 	public Button buttonView;
 	public TextView freqView;
 	
 	}
 
 
 	}
