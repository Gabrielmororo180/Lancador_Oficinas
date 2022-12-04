#include <SoftwareSerial.h> //INCLUSÃO DE BIBLIOTECA
 
const int pinoRX = 13; //PINO DIGITAL 2 (RX)
const int pinoTX = 12; //PINO DIGITAL 3 (TX)
int dadoBluetooth = 0; 

SoftwareSerial bluetooth(pinoRX, pinoTX); //PINOS QUE EMULAM A SERIAL, ONDE
//O PINO 13 É O RX E O PINO 12 É O TX

//motor_A
int IN1 = 2 ;
int IN2 = 4 ;
int velocidadeA = 3;
int velocidade = 0;
//Inicializa Pinos
#include <Stepper.h> 
 
const int stepsPerRevolution = 500; 
  
//Inicializa a biblioteca utilizando as portas de 8 a 11 para 
//ligacao ao motor 
Stepper myStepper(stepsPerRevolution, 8,10,9,11); 
  
void setup(){
  Serial.begin(9600); //INICIALIZA A SERIAL
  bluetooth.begin(9600); //INICIALIZA A SERIAL DO BLUETOOTH
  pinMode(IN1,OUTPUT);
  pinMode(IN2,OUTPUT);
  pinMode(velocidadeA,OUTPUT);
  myStepper.setSpeed(60);

}
void loop(){
   
 //MOTOR DE PASSOS
 myStepper.setSpeed(60);
 myStepper.step(60);

 
 if(bluetooth.available()){ //SE O BLUETOOTH ESTIVER HABILITADO, FAZ
       
         dadoBluetooth = bluetooth.read();
       Serial.println("AQUI");
 } //VARIÁVEL RECEBE O VALOR ENVIADO PELO BLUETOOTH
  
   
if (dadoBluetooth == '1'){
  //VELOCIDADE MÁXIMA
  Serial.println("ligar");
  digitalWrite(IN1,HIGH);
  digitalWrite(IN2,LOW);
  analogWrite(velocidadeA,230);
 }

    if (dadoBluetooth == '2'){
  //DELIGADO
  Serial.println("Desligar");
  digitalWrite(IN1,LOW);
  digitalWrite(IN2,LOW);
  }

  if (dadoBluetooth == '3'){
  //VELOCIDADE MÉDIA
  Serial.println("MÉDIA");
  digitalWrite(IN1,HIGH);
  digitalWrite(IN2,LOW);
  analogWrite(velocidadeA,180);

  }

  if (dadoBluetooth == '4'){
   //VELOCIDADE MINIMA
  Serial.println("MINIMA");
  digitalWrite(IN1,HIGH);
  digitalWrite(IN2,LOW);
  analogWrite(velocidadeA,120);
  }
}
