/******************************************************************************
                         ========== Assumption =======
								   
            The first line of the input file is in the following form:
			number <tab> number <end of line>

            As soon as the last line of the input file finishes don't
            press <Enter> or Anything.                                     */
/******************************  Final Version  ********************************

	Rocky Patel
	
******************************************************************************/
/*                          Program Description

		This program simulates Round Robin Scheduling Algorithm.  The input file,
		time quantum and total time to run this program are taken using command
		line arguments from the user.  The processes read form the input file
		are processed on as stucts on linked list.  The JOB QUEUE provides all
		the processes beginning from time zero till total time to the READY
		QUEUE.  READY QUEUE runs process in the RUN STATE.  From RUN STATE
		either process makes IOCALL and goes to the DISK QUEUE or DISPATCHes and
		goes back to the tail of the READY QUEUE.

		The OUTPUT consist of the following:
			JOB QUEUE is outputted first.  Program outputs Gant chart second.
			READY QUEUE and DISK QUEUE are outputted last.
******************************************************************************/
#include<fstream.h>
#include<iomanip.h>
#include<stdlib.h>          //Libray declaration
#include<stddef.h>
#include<string.h>


struct process
{
	char pro[5],rdy[5],blk[5],dis[5];  //Linked list declaration and initiation
	int at,bl,ar,br,aj,bj,ah,bh;
	process * next;
};

int a[1000];       //Array declaration which reads the input file
int b[1000];

typedef process* nptr;
nptr firstptr;         //Declaration of pointers that process JOB QUEUE
nptr lastptr;
nptr head;             //Declaration of pointers that process READY QUEUE
nptr tail;
nptr Dev_h;            //Declaration of pointers that process DISK QUEUE
nptr Dev_t;
nptr Dpt_h;            //Declaration of pointers for Dispatching the process
nptr Dpt_t;            //from RUN STATE.

void Delete(nptr& tptr);             //Deletes the head of linked list
void Print(nptr& head,int tq,int freq,int total); //Processes RUN STATE
void Qprint (nptr& head);             //Outputs DISK QUEUE
void QprintRdy (nptr& head);          //Outputs READY QUEUE
void Blockq(nptr& head,int clock);    //Processes DISK QUEUE
void DisPatch(nptr& head,int clock);  //Takes care of DISPATCHING

int tim=0;           //Declaration and initiation of system time
int clock=0,IOcall;  //Declaration and initiation of clock and IOCALL time.

/*=========================== START OF MAIN =================================*/

int main(int argc,char *argv[])
{
	char p[1000][5];     //character array that reads processes from input file
	int i=0;
	int tq,total,freq; //Declaration of TIME QUANTUM, TOTAL TIME, IO FREQUENCY

  /***************** READING INPUT FILE AND COMMAND LINE ********************/

	ifstream fin;     //using input file

	if ((argv[1]!=NULL)&&(argv[2]!=NULL)&&(argv[3]!=NULL))
	{                         //checks the command line arguments for errors
		fin.open(argv[1]);     //opens the input file
		tq=atoi(argv[2]);      //takes in TIME QUANTUM and converts to integer
		total=atoi(argv[3]);   //takes in TOTAL TIME and converts to integer

		fin>>IOcall>>freq;     //reads IOCALL and IO FREQUENCY
		while(!fin.eof())      //reads till the end of input file
		{
			fin>>p[i];    //reads PROCESSES from input file
			fin>>a[i];    //reads ARRIVAL TIME from input file
			fin>>b[i];    //reads BURST LENGTH from input file
			i++;
		}
		fin.close();     //closes input file
		i=i-2;

	 /************************ JOB QUEUE PROCESSING**************************/

		nptr tptr;      //declaration of temporary pointer

		cout<<"\n\nJOB HEAD";     //JOB QUEUE output
		if (i>0)		     //if the array is not empty
		{	firstptr = new process();     //creating new node in linked list
			strcpy(firstptr->pro,p[0]); //filling JOB QUEUE at head with processes
			firstptr->at = a[0];        //filling at head ARRIVAL TIME
			firstptr->bl = b[0];        //filling at head BURST LENGTH
			tptr=firstptr;
		}
		cout<<"->"<<firstptr->pro;     //outputs the first process

		for (int y=1; y<=i;y++)    //filling processes at the tail of JOB QUEUE
		{
			tptr->next = new process;  //creating new node in JOB QUEUE
			tptr=tptr->next;
			strcpy(tptr->pro,p[y]);    //processes at the tail
			tptr->at = a[y];           //ARRIVAL TIME at tail
			tptr->bl = b[y];           //BURST LENGTH at tail
			cout<<"->"<<tptr->pro;
		}
		lastptr=tptr;                   //Outputing JOB QUEUE
		cout<<"<-TAIL"<<endl<<endl;

	 /******************* JOB QUEUE TO READY QUEUE **************************/

		cout<<"\n      Process    Time"<<endl<<endl;
												//outputs titles of gant chart 
		while (tim != total)  //MAIN LOOP of the program that checks TOTAL TIME
		{                     //and system time. If equal then stops running
			if(firstptr != NULL)   //checks head of JOB QUEUE for processes
			{
				if (firstptr->at == tim)  //checks process coming from JOB QUEUE
				{
					if(head != NULL)    //checks head of READY QUEUE
					{
						tail->next = new process;   //new node for READY QUEUE
						tail = tail->next;
						strcpy(tail->rdy,firstptr->pro);//copies PROCESS, ARRIVAL
						tail->ar = firstptr->at;  //TIME and BURST LENGTH from JOB
						tail->br = firstptr->bl;  //QUEUE to READY QUEUE
						tail->next = NULL;
					}
					if (head == NULL)     //check READY QUEUE for no processes
					{
						head = new process;     //new node for READY QUEUE
						strcpy(head->rdy,firstptr->pro); //copies PROCESS, ARRIVAL
						head->ar = firstptr->at;   //TIME and BURST LENGTH from JOB
						head->br = firstptr->bl;   //QUEUE to READY QUEUE if head
						head->next = NULL;         // is NULL
						tail = head;
					}
					Delete(firstptr);   //deletes head of JOB QUEUE
				}
			}

	 /********************** DISK QUEUE TO READY QUEUE *********************/

			if (Dev_h != NULL)       //checks head of DISK QUEUE for processes
				{
					if (Dev_h->aj == tim)  //checks process coming from DISK QUEUE
					{
						if(head != NULL)    //checks head of READY QUEUE
						{
							tail->next = new process;   //new node for READY QUEUE
							tail = tail->next;
							strcpy(tail->rdy,Dev_h->blk); //copies PROCESS, ARRIVAL
							tail->ar = Dev_h->aj;   //TIME and BURST LENGTH from DISK
							tail->br = Dev_h->bj;    //QUEUE to READY QUEUE
							tail->next = NULL;
						}
						if (head == NULL)     //check READY QUEUE for no processes
						{
							head = new process;   //new node for READY QUEUE
							strcpy(head->rdy,Dev_h->blk); //copies PROCESS, ARRIVAL
							head->ar = Dev_h->aj;   //TIME and BURST LENGTH from DISK
							head->br = Dev_h->bj;  //QUEUE to READY QUEUE if head
							head->next = NULL;     // is NULL
							tail = head;
						}
							Delete(Dev_h);        //deletes head of DISK QUEUE
					}
				}

	 /************************ DISPATCH TO READY QUEUE *********************/

			if (Dpt_h != NULL)       //checks head of DISPATCH for processes
				{
					if (Dpt_h->ah == tim)   //checks process coming from DISPATCH
					{
						if(head != NULL)     //checks head of READY QUEUE
						{
							tail->next = new process;  //new node for READY QUEUE
							tail = tail->next;
							strcpy(tail->rdy,Dpt_h->dis); //copies PROCESS, ARRIVAL
							tail->ar = Dpt_h->ah;    //TIME and BURST LENGTH from
							tail->br = Dpt_h->bh;    //DISPATCH to READY QUEUE
							tail->next = NULL;
						}
						if (head == NULL)     //check READY QUEUE for no processes
						{
							head = new process;    //new node for READY QUEUE
							strcpy(head->rdy,Dpt_h->dis); //copies PROCESS, ARRIVAL
							head->ar = Dpt_h->ah;   //TIME and BURST LENGTH from
							head->br = Dpt_h->bh;   //DISPATCH to READY QUEUE if head
							head->next = NULL;      // is NULL
							tail = head;
						}
							Delete(Dpt_h);    //deletes head of DISPATCH
					}
				}
			if(clock != total)  //checks TOTAL TIME equal clock
				{
					Print(head,tq,freq,total);  //call to RUN STATE
				}
			if (tim == clock)   //check system time and clock for equality
				{
					cout<<setw(10)<<"  "<<setw(10)<<clock<<endl;  //outputs clock
					clock++;           //increaments clock
				}
			tim++;       //increaments system time
		}
	}
	else       //if command line arguments are passed wrongly outputs
	{  cout<<"\n\nError on command line arguments is encountered"<<endl;
		cout<<"Please check command line arguments you have entered";
	}
	if (clock == total)       //checks TOTAL TIME equal clock
	{
		Print(head,tq,freq,total);    //call to RUN STATE
	}
	Delete(head);        //deletes node at the head of READY QUEUE
	QprintRdy (head);    //outputs READY QUEUE
	Qprint (Dev_h);      //outputs DISK QUEUE
	return 0;
}

/*============================  END OF MAIN  ===============================*/

void Delete(nptr& firstptr) //deletes the node at the head of the linked list
{                            //erases the head
	nptr tptr = firstptr;
	if (firstptr != NULL)	//if not the last node
		firstptr = firstptr->next;
		delete tptr;
}

/*===================  RUN STATE AND GANT CHART HANDLING  ===================*/

void Print(nptr& head,int tq,int freq,int total)
{                            //Operates RUN STATE upon call
	if (head != NULL)        //checks head of READY QUEUE for coming processes
	{
		cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
									  //outputs process and corresponding clock
		if (clock < total)     //if clock is less than TOTAL TIME
		{	if (head->br <= tq) //If BURST LENGTH less than or equal TIME QUANTUM
			{  int w=head->br;
				for (int m=0; m < head->br; m++)
				{
					clock++;      //increaments clock according to BURST LENGTH

					if (clock % freq==0) //check for IOCALL
					{	m++;
						head->br = (head->br) - m;  //modify BURST LENGTH

						if(head->br != 0)   //check BURST LENGTH for zero
						{
							cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
							cout<<"disk   ~~~~~~~~~~~~~"<<endl; //output gant chart
							Blockq(head,clock);  //call to DISK QUEIUE processing
						}
						else if(head->br == 0) //check BURST LENGTH for zero
						{
							cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
							cout<<"       ~~~~~~~~~~~~~  <-No disk call made since "<<
								head->rdy<<" finishes tq exactly at "<<clock<<endl; 
						}                                        //output gant chart
						break;        //exit the for loop
					}
					else if(clock % freq !=0 && w == m+1)//if no IOCALL made
					{
						cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
						cout<<"       ~~~~~~~~~~~~~"<<endl;  //output gant chart
						break;  //exit the loop
					}
					if(clock == total )  //clock equals TOTAL TIME
					{
						break;   //exit the loop
					}
				}
			}
			else if (head->br > tq) //if BURST LENGTH greater than TIME QUANTUM
			{
				for (int n = 0; n < tq; n++)
				{
					clock++;       			//Increaments clock up to TIME QUANTUM
					if (clock % freq==0)  //IOCALL made
					{  n++;
						head->br = (head->br) - n; //Modify BURST LENGTH
						if(head->br != 0)   //BURST LENGTH not equal to zero
						{
							cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
							cout<<"disk   ~~~~~~~~~~~~~"<<endl; //Output gant chart
							Blockq(head,clock); //call for DISK QUEUE processing
						}
						else if(head->br == 0)  //if BURST LENGTH equal to zero
						{
							cout<<"       ~~~~~~~~~~~~~  <-No disk call made since "<<
								head->rdy<<" finishes tq exactly at "<<clock<<endl; 
						}                                        //output gant chart
						break;      //exit loop
					}
					else if(clock % freq !=0 && tq == n+1)   //No IOCALL made
					{
						head->br = (head->br)-tq;  //Modify BURST LENGTH
						if(head->br != 0)          //BURST LENGTH not zero
						{
							cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
							cout<<"       ~~~~~~~~~~~~~"<<endl;  //Output gant chart
							DisPatch(head,clock);  //call to DISPATCH processing
						}
						else if(head->br == 0) //BURST LENGTH is zero
						{
							cout<<setw(10)<<head->rdy<<setw(10)<<clock<<endl;
							cout<<"       ~~~~~~~~~~~~~"<<endl;  //output gant chart
						}
						break;      //exit loop
					}
					if(clock == total)   //If clock equal TOTAL TIME
					{
						break;    //Exit loop
					}
				}
			}
			Delete(head);    //Deletes node at the head of READY QUEUE
		}
	}
}

/*=======================  DISK QUEUE HANDLING  =============================*/

void Blockq(nptr& head,int clock)
{
	if(Dev_h != NULL) //If head of DISK QUEUE is not empty
	{
		Dev_t->next = new process;  //create new node
		Dev_t = Dev_t->next;
		Dev_t->aj = IOcall+clock; //Assign waiting time
		Dev_t->bj = head->br;     //Assign remaining BURST LENGTH
		strcpy(Dev_t->blk,head->rdy); //Assign incoming process
		Dev_t->next = NULL;
	}
	if (Dev_h == NULL)   //If DISK QUEUE head is empty
	{
		Dev_h = new process;        //create new node
		Dev_h->aj = IOcall+clock;   //Assign waiting time
		Dev_h->bj = head->br;          //Assign remaining BURST LENGTH
		strcpy(Dev_h->blk,head->rdy);   //Assign incoming process
		Dev_h->next = NULL;
		Dev_t = Dev_h;              //head to tail
	}
}

/*=========================  DISPATCH HANDLING  =============================*/

void DisPatch(nptr& head,int clock)
{
	if(Dpt_h != NULL)        //If head of DISPATCH is not empty
	{
		Dpt_t->next = new process;    //create new node
		Dpt_t = Dpt_t->next;
		Dpt_t->ah = clock;            //Assign NEW ARRIVAL time
		Dpt_t->bh = head->br;          //Assign remaining BURST LENGTH
		strcpy(Dpt_t->dis,head->rdy);  //Assign incoming process
		Dpt_t->next = NULL;
	}
	if (Dpt_h == NULL)       //If head of DISPATCH is empty
	{
		Dpt_h = new process;           //create new node
		Dpt_h->ah = clock;             //Assign NEW ARRIVAL time
		Dpt_h->bh = head->br;          //Assign remaining BURST LENGTH
		strcpy(Dpt_h->dis,head->rdy);   //Assign incoming process
		Dpt_h->next = NULL;
		Dpt_t = Dpt_h;              //head to tail
	}
}

/*===================== PRINTING DISK QUEUE TO SCREEN ======================*/

void Qprint (nptr& head)
{
	nptr currentptr=head;   //Assigns head to temporary pointer
	if (currentptr ==0)     //If list(Queue) is empty
	{	cout<<"Disk Queue is empty"<<endl;  //Output
	}
	else             //If list(Queue) is not empty
	{	cout<<"DSK_HEAD";
		while(currentptr != 0)  //Until list(Queue) is empty
		{
			cout<<"->"<<currentptr->blk;   //output processes in the DISK QUEUE
			currentptr = currentptr->next;
		}
		cout<<"<-TAIL"<<endl;
	}
	cout<<"\n";
}

/*===================== PRINTING READY QUEUE TO SCREEN ======================*/

void QprintRdy (nptr& head)
{
	nptr currentptr=head;     //Assigns head to temporary pointer
	if (currentptr ==0)            //If list(Queue) is empty
	{	cout<<"\n\nReady Queue is empty"<<endl;    //Output
	}
	else                   //If list(Queue) is not empty
	{	cout<<"\n\nRDY_HEAD";
		while(currentptr != 0)    //Until list(Queue) is empty
		{
			cout<<"->"<<currentptr->rdy;
			currentptr = currentptr->next;  //output processes in the READY QUEUE
		}
		cout<<"<-TAIL"<<endl;
	}
	cout<<"\n";
}

/*========================  END OF PROGRAM  =================================*/
