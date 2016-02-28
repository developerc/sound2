import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by saperov on 28.02.16.
 */
public class myGUI {
    private JButton captureButton;
    private JButton stopButton;
    private JButton playbackButton;
    private JTextArea textArea1;
    private JPanel panel;
    private JButton levelButton;

    boolean stopCapture = false;
    ByteArrayOutputStream byteArrayOutputStream;
    AudioFormat audioFormat;
    TargetDataLine targetDataLine;
    AudioInputStream audioInputStream;
    SourceDataLine sourceDataLine;
    int rmsLevel;


    public static void main(String[] args){

        JFrame frame = new JFrame("myGUI");
        frame.setContentPane(new myGUI().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(350,150);
        frame.setVisible(true);
    }

    public myGUI() {
        captureButton.setEnabled(true);
        stopButton.setEnabled(false);
        playbackButton.setEnabled(false);

        captureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureButton.setEnabled(false);
                stopButton.setEnabled(true);
                textArea1.append("Pressed captureButton\n");
                captureButton.setEnabled(false);
                stopButton.setEnabled(true);
                playbackButton.setEnabled(false);
                //Capture input data from the microphone until the Stop button is clicked.
                captureAudio();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playbackButton.setEnabled(true);
                stopButton.setEnabled(false);
                textArea1.append("Pressed stopButton\n");
                stopButton.setEnabled(false);
                playbackButton.setEnabled(true);
                captureButton.setEnabled(true);
                //Terminate the capturing of input data from the microphone.
                stopCapture = true;
            }
        });

        playbackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playbackButton.setEnabled(false);
                captureButton.setEnabled(true);
                textArea1.append("Pressed playbackButton\n");
                playbackButton.setEnabled(true);
                captureButton.setEnabled(true);
                stopButton.setEnabled(false);
                //Play back all of the data that was saved during capture.
                playAudio();
            }
        });
        levelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea1.append("level=" + rmsLevel+ "\n");
            }
        });
    }

    //This method plays back the audio data that
    // has been saved in the ByteArrayOutputStream
    private void playAudio() {
        try{
            //Get everything set up for playback.
            //Get the previously-saved data into a byte array object.
            byte audioData[] = byteArrayOutputStream.toByteArray();
            //Get an input stream on the byte array containing the data
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = getAudioFormat();
            audioInputStream = new AudioInputStream(
                    byteArrayInputStream,
                    audioFormat,
                    audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(
                            SourceDataLine.class,
                            audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            //Create a thread to play back the data and start it  running.  It will run until
            // all the data has been played back.
            Thread playThread = new PlayThread();
            playThread.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }//end catch
    }//end playAudio

    //This method captures audio input from a microphone and saves it in a
    // ByteArrayOutputStream object.
    private void captureAudio() {
        try{
            //Get and display a list of available mixers
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            System.out.println("Available mixers:");
            for(int cnt = 0; cnt < mixerInfo.length;
                cnt++){
                System.out.println(mixerInfo[cnt].getName());
            }//end for loop

            //Get everything set up for capture
            audioFormat = getAudioFormat();

            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            //Select one of the available mixers.
            Mixer mixer = AudioSystem.getMixer(mixerInfo[0]);
            System.out.println("Used mixer:" + mixerInfo[0]);

            //Get a TargetDataLine on the selected mixer.
            targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            //Prepare the line for use.
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            //Create a thread to capture the microphone data and start it running.  It will run
            // until the Stop button is clicked.
            Thread captureThread = new CaptureThread();
            captureThread.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }//end catch
    }//end captureAudio method

    //This method creates and returns an AudioFormat object for a given set of format
    // parameters.  If these parameters don't work  well for you, try some of the other
    // allowable parameter values, which are shown in comments following the declartions.
    private AudioFormat getAudioFormat(){
        float sampleRate = 8000.0F;
        //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;
        //8,16
        int channels = 1;
        //1,2
        boolean signed = true;
        //true,false
        boolean bigEndian = false;
        //true,false
        return new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
    }//end getAudioFormat
//=============================================//

    //Inner class to capture data from microphone
    class CaptureThread extends Thread{
        //An arbitrary-size temporary holding buffer
        byte tempBuffer[] = new byte[10000];
        public void run(){
            byteArrayOutputStream = new ByteArrayOutputStream();
            stopCapture = false;
            try{   //Loop until stopCapture is set by another thread that services the Stop button.
                while(!stopCapture){
                    //Read data from the internal buffer of the data line.
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if(cnt > 0){
                        //Save data in output stream object.
                        byteArrayOutputStream.write(tempBuffer, 0, cnt);
                        rmsLevel = calculateRMSLevel(tempBuffer);
                    }//end if
                }//end while
                byteArrayOutputStream.close();
            }catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }//end catch
        }//end run
    }//end inner class CaptureThread
//===================================//

    //Inner class to play back the data that was saved.
    class PlayThread extends Thread{
        byte tempBuffer[] = new byte[10000];

        public void run(){
            try{
                int cnt;
                //Keep looping until the input read method returns -1 for empty stream.
                while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
                    if(cnt > 0){
                        //Write data to the internal buffer of the data line where it will be
                        // delivered to the speaker.
                        sourceDataLine.write(tempBuffer,0,cnt);
                    }//end if
                }//end while
                //Block and wait for internal buffer of the data line to empty.
                sourceDataLine.drain();
                sourceDataLine.close();
            }catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }//end catch
        }//end run
    }//end inner class PlayThread
//=============================================//

    private int calculateRMSLevel(byte[] audioData)
    {
        // audioData might be buffered data read from a data line
        long lSum = 0;
        for(int i=0; i<audioData.length; i++)
            lSum = lSum + audioData[i];

        double dAvg = lSum / audioData.length;

        double sumMeanSquare = 0d;
        for(int j=0; j<audioData.length; j++)
            sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);

        double averageMeanSquare = sumMeanSquare / audioData.length;
        return (int)(Math.pow(averageMeanSquare,0.5d) + 0.5);
    }
}
