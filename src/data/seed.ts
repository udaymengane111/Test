import type { AppData } from '../types';

const classDefs = [
  { id: 'c1a', standard: 1, section: 'A', classTeacher: 'Mrs. Sunita Patil' },
  { id: 'c1b', standard: 1, section: 'B', classTeacher: 'Ms. Priya Deshmukh' },
  { id: 'c2a', standard: 2, section: 'A', classTeacher: 'Mrs. Kavita Joshi' },
  { id: 'c2b', standard: 2, section: 'B', classTeacher: 'Mr. Rajesh Kulkarni' },
  { id: 'c3a', standard: 3, section: 'A', classTeacher: 'Mrs. Meena Sharma' },
  { id: 'c3b', standard: 3, section: 'B', classTeacher: 'Ms. Anjali More' },
  { id: 'c4a', standard: 4, section: 'A', classTeacher: 'Mr. Suresh Pawar' },
  { id: 'c5a', standard: 5, section: 'A', classTeacher: 'Mrs. Deepa Nair' },
];

const namesByClass: Record<string, { name: string; gender: 'boy' | 'girl'; parent: string }[]> = {
  c1a: [
    { name: 'Aarav Sharma', gender: 'boy', parent: 'Vikram Sharma' },
    { name: 'Ananya Patel', gender: 'girl', parent: 'Nisha Patel' },
    { name: 'Vihaan Reddy', gender: 'boy', parent: 'Srinivas Reddy' },
    { name: 'Diya Gupta', gender: 'girl', parent: 'Anita Gupta' },
    { name: 'Arjun Singh', gender: 'boy', parent: 'Ramesh Singh' },
    { name: 'Isha Verma', gender: 'girl', parent: 'Pooja Verma' },
    { name: 'Kabir Mehta', gender: 'boy', parent: 'Amit Mehta' },
    { name: 'Myra Joshi', gender: 'girl', parent: 'Kavita Joshi' },
    { name: 'Reyansh Kumar', gender: 'boy', parent: 'Sanjay Kumar' },
    { name: 'Sara Khan', gender: 'girl', parent: 'Fatima Khan' },
    { name: 'Advait Nair', gender: 'boy', parent: 'Deepa Nair' },
    { name: 'Kiara Das', gender: 'girl', parent: 'Rina Das' },
  ],
  c1b: [
    { name: 'Ishaan Pillai', gender: 'boy', parent: 'Ravi Pillai' },
    { name: 'Aanya Iyer', gender: 'girl', parent: 'Lakshmi Iyer' },
    { name: 'Vivaan Rao', gender: 'boy', parent: 'Prakash Rao' },
    { name: 'Pari Choudhury', gender: 'girl', parent: 'Sneha Choudhury' },
    { name: 'Atharv Bhosale', gender: 'boy', parent: 'Mahesh Bhosale' },
    { name: 'Navya Kulkarni', gender: 'girl', parent: 'Smita Kulkarni' },
    { name: 'Shaurya Desai', gender: 'boy', parent: 'Nikhil Desai' },
    { name: 'Anvi Gaikwad', gender: 'girl', parent: 'Sunita Gaikwad' },
    { name: 'Dhruv Banerjee', gender: 'boy', parent: 'Arnab Banerjee' },
    { name: 'Riya Chatterjee', gender: 'girl', parent: 'Mita Chatterjee' },
  ],
  c2a: [
    { name: 'Krishna Yadav', gender: 'boy', parent: 'Mohan Yadav' },
    { name: 'Saanvi Agarwal', gender: 'girl', parent: 'Rekha Agarwal' },
    { name: 'Omkar Jadhav', gender: 'boy', parent: 'Balu Jadhav' },
    { name: 'Tara Menon', gender: 'girl', parent: 'Shalini Menon' },
    { name: 'Yash Thakur', gender: 'boy', parent: 'Dinesh Thakur' },
    { name: 'Aditi Saxena', gender: 'girl', parent: 'Geeta Saxena' },
    { name: 'Rohan Kapoor', gender: 'boy', parent: 'Ajay Kapoor' },
    { name: 'Mira Sen', gender: 'girl', parent: 'Bina Sen' },
    { name: 'Dev Malhotra', gender: 'boy', parent: 'Kunal Malhotra' },
    { name: 'Zara Sheikh', gender: 'girl', parent: 'Imran Sheikh' },
    { name: 'Harsh Pandey', gender: 'boy', parent: 'Alok Pandey' },
  ],
  c2b: [
    { name: 'Lakshya Jain', gender: 'boy', parent: 'Vipul Jain' },
    { name: 'Nisha Bhat', gender: 'girl', parent: 'Meera Bhat' },
    { name: 'Ayaan Qureshi', gender: 'boy', parent: 'Farhan Qureshi' },
    { name: 'Prisha Kotak', gender: 'girl', parent: 'Hetal Kotak' },
    { name: 'Neel Trivedi', gender: 'boy', parent: 'Chirag Trivedi' },
    { name: 'Ira Phadke', gender: 'girl', parent: 'Ashwini Phadke' },
    { name: 'Samar Hegde', gender: 'boy', parent: 'Ganesh Hegde' },
    { name: 'Keya Dutta', gender: 'girl', parent: 'Soma Dutta' },
  ],
  c3a: [
    { name: 'Rudra Chauhan', gender: 'boy', parent: 'Vijay Chauhan' },
    { name: 'Avni Shukla', gender: 'girl', parent: 'Madhuri Shukla' },
    { name: 'Parth Gokhale', gender: 'boy', parent: 'Ashok Gokhale' },
    { name: 'Jia Fernandes', gender: 'girl', parent: 'Maria Fernandes' },
    { name: 'Veer Solanki', gender: 'boy', parent: 'Hardik Solanki' },
    { name: 'Shanaya Bose', gender: 'girl', parent: 'Rupa Bose' },
    { name: 'Aryan Naidu', gender: 'boy', parent: 'Venkat Naidu' },
    { name: 'Myra Kale', gender: 'girl', parent: 'Savita Kale' },
    { name: 'Kabir Rane', gender: 'boy', parent: 'Pravin Rane' },
    { name: 'Anika Ghosh', gender: 'girl', parent: 'Papia Ghosh' },
  ],
  c3b: [
    { name: 'Tanay Mishra', gender: 'boy', parent: 'Suresh Mishra' },
    { name: 'Lavanya Iyer', gender: 'girl', parent: 'Padma Iyer' },
    { name: 'Arnav Sawant', gender: 'boy', parent: 'Deepak Sawant' },
    { name: 'Siya Raut', gender: 'girl', parent: 'Jyoti Raut' },
    { name: 'Yuvraj Patil', gender: 'boy', parent: 'Sambhaji Patil' },
    { name: 'Aadhya Kadam', gender: 'girl', parent: 'Manisha Kadam' },
    { name: 'Reyansh Shetty', gender: 'boy', parent: 'Pradeep Shetty' },
    { name: 'Naina Pillai', gender: 'girl', parent: 'Kavitha Pillai' },
  ],
  c4a: [
    { name: 'Aditya Barve', gender: 'boy', parent: 'Nitin Barve' },
    { name: 'Khushi Dixit', gender: 'girl', parent: 'Seema Dixit' },
    { name: 'Virat Chavan', gender: 'boy', parent: 'Ravi Chavan' },
    { name: 'Pihu Bhatt', gender: 'girl', parent: 'Nidhi Bhatt' },
    { name: 'Shaurya Raina', gender: 'boy', parent: 'Anil Raina' },
    { name: 'Anaya Kohli', gender: 'girl', parent: 'Priya Kohli' },
    { name: 'Kunal Waghmare', gender: 'boy', parent: 'Bharat Waghmare' },
    { name: 'Meher Sidhu', gender: 'girl', parent: 'Gurpreet Sidhu' },
    { name: 'Aarush Bhattacharya', gender: 'boy', parent: 'Debashish Bhattacharya' },
    { name: 'Tanya Varma', gender: 'girl', parent: 'Shilpa Varma' },
    { name: 'Pranav Dhar', gender: 'boy', parent: 'Ashim Dhar' },
    { name: 'Ritika Suri', gender: 'girl', parent: 'Neelam Suri' },
  ],
  c5a: [
    { name: 'Aryan Khatri', gender: 'boy', parent: 'Manoj Khatri' },
    { name: 'Ishita Banik', gender: 'girl', parent: 'Chitra Banik' },
    { name: 'Daksh Oberoi', gender: 'boy', parent: 'Sandeep Oberoi' },
    { name: 'Sia Mukherjee', gender: 'girl', parent: 'Anjali Mukherjee' },
    { name: 'Kartik Bendre', gender: 'boy', parent: 'Umesh Bendre' },
    { name: 'Aarohi Deshpande', gender: 'girl', parent: 'Vaishali Deshpande' },
    { name: 'Rohan Thombre', gender: 'boy', parent: 'Govind Thombre' },
    { name: 'Vanya Krishnan', gender: 'girl', parent: 'Radha Krishnan' },
    { name: 'Manav Tiwari', gender: 'boy', parent: 'Harish Tiwari' },
    { name: 'Aisha Begum', gender: 'girl', parent: 'Nasreen Begum' },
    { name: 'Siddharth Rao', gender: 'boy', parent: 'Mohan Rao' },
    { name: 'Pooja Nikam', gender: 'girl', parent: 'Lata Nikam' },
    { name: 'Harshwardhan Shinde', gender: 'boy', parent: 'Vikas Shinde' },
    { name: 'Tanvi Apte', gender: 'girl', parent: 'Ranjana Apte' },
  ],
};

function phoneFor(index: number): string {
  const base = 9800000000 + index * 137 + ((index * 17) % 900);
  return String(base).slice(0, 10);
}

export function createSeedData(): AppData {
  const students = Object.entries(namesByClass).flatMap(([classId, list]) =>
    list.map((s, i) => ({
      id: `${classId}-s${i + 1}`,
      rollNo: i + 1,
      name: s.name,
      gender: s.gender,
      classId,
      parentName: s.parent,
      parentPhone: phoneFor(classId.charCodeAt(1) * 100 + i),
      admissionNo: `ADM${2024 + Math.min(5, parseInt(classId[1], 10))}${String(i + 1).padStart(3, '0')}`,
      active: true,
    })),
  );

  return {
    school: {
      name: 'Shri Saraswati Vidyalaya',
      academicYear: '2025–26',
      medium: 'English / Marathi',
      address: 'Sector 12, Nerul, Navi Mumbai, Maharashtra',
    },
    classes: classDefs,
    students,
    attendance: [],
  };
}

export function classLabel(standard: number, section: string): string {
  return `Class ${standard}${section}`;
}
