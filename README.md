
# Generate PDF file and send it to a printer

THe IP / port used as the printer are hard coded in the app. 
If using socat, check your IP (`ifconfig` => find your computer's "inet" address that is not 127.0.0.1, eg "inet 192.168.1.32")
If using a real printer, get it's IP.

Both the Android device on which the PoC is running and the "printer" (real or emulated) must be on the **same WiFi network**

Send PDF file via TCP socket:
To use an emulated printer, eg socat
- set up socat (brew install socat)
- launch socat to listen to port 9100, and save all it receives to a local file:
```
socat -u TCP-LISTEN:9100,reuseaddr,fork OPEN:/tmp/received_printjob.pdf,creat,append
```
<img width="325" alt="Screenshot 2025-06-20 at 17 07 29" src="https://github.com/user-attachments/assets/2763dac6-256d-49ef-9f9c-dd9589bbc590" />

- From the app, click generate PDF ( first check mark should turn green)

<img width="325" alt="Screenshot 2025-06-20 at 17 07 40" src="https://github.com/user-attachments/assets/cae61436-d0b4-43b5-bb16-ae55e826158e" />

- Then click to send PDF to printer, this launches a file picker, you need to have some random pdf file present, choose it, it will not actually be used (yes, my PoC is not very smooth) => second checkmark should turn green if all goes well, or look in Logcat

<img width="325" alt="Screenshot 2025-06-20 at 17 07 55" src="https://github.com/user-attachments/assets/d5aca732-369d-411d-a4de-22cbee22e1b8" />

- check that socat received your PDF in the chosen local file on the computer

<img width="373" alt="Screenshot 2025-06-20 at 17 09 05" src="https://github.com/user-attachments/assets/2354faa0-73b6-4c0d-8b6c-3112db00c42b" />
<img width="432" alt="Screenshot 2025-06-15 at 22 49 52" src="https://github.com/user-attachments/assets/cd3b393c-7832-4999-ad04-cc2b0db1dc6d" />

- if you have the IP of a printer, use that instead, with the matching port on which the printer's server is listening

# Send ZPL with QR code via TCP

- click on the ZPL + QR code button, again, the IP of the target printer is hard-coded in the PoC app, so you may need to adapt it for your printer.
  
