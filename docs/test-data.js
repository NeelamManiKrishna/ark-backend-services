// =============================================================
// ARK Test Data - Bulk Insert Script (v2)
// Run: docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web test-data.js
// Or copy to container first:
//   docker cp src/main/resources/test-data.js mongodb:/tmp/test-data.js
//   docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web /tmp/test-data.js
//
// Password for all users: Password@123
// =============================================================

// Clean existing data
db.organizations.drop();
db.branches.drop();
db.academic_classes.drop();
db.students.drop();
db.faculty.drop();
db.users.drop();
db.audit_logs.drop();
db.examinations.drop();
db.exam_subjects.drop();
db.exam_results.drop();

// BCrypt hash of "Password@123"
const bcryptHash = "$2a$10$0V4TSH2bEuUh/x6ctUj1teDES0mNyyfhYd9i6Mb5SHC0tuBF1qmJW";

// ARK ID generator (uppercase alphanumeric, 8 chars)
const CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
function nanoid() {
  let result = "";
  for (let i = 0; i < 8; i++) {
    result += CHARS.charAt(Math.floor(Math.random() * CHARS.length));
  }
  return result;
}

// =============================================================
// Organizations (3 orgs: 1 ACTIVE, 1 ACTIVE, 1 INACTIVE)
// =============================================================
print("--- Inserting Organizations ---");

const orgs = db.organizations.insertMany([
  {
    arkId: "ARK-ORG-" + nanoid(),
    name: "Springfield Academy",
    address: "100 Education Blvd, Springfield, IL 62704",
    contactEmail: "admin@springfieldacademy.edu",
    contactPhone: "+1-217-555-0100",
    website: "https://springfieldacademy.edu",
    logoUrl: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    arkId: "ARK-ORG-" + nanoid(),
    name: "Riverside College",
    address: "250 River Road, Austin, TX 73301",
    contactEmail: "info@riversidecollege.edu",
    contactPhone: "+1-512-555-0200",
    website: "https://riversidecollege.edu",
    logoUrl: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    arkId: "ARK-ORG-" + nanoid(),
    name: "Oakwood International School",
    address: "500 Oak Avenue, Portland, OR 97201",
    contactEmail: "contact@oakwoodintl.edu",
    contactPhone: "+1-503-555-0300",
    website: "https://oakwoodintl.edu",
    logoUrl: null,
    status: "INACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

const orgIds = Object.values(orgs.insertedIds);
print(`Inserted ${orgIds.length} organizations`);

// =============================================================
// Branches (6 branches across 3 orgs)
// =============================================================
print("--- Inserting Branches ---");

const branches = db.branches.insertMany([
  // Springfield Academy — 2 branches
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[0].toString(),
    name: "Main Campus",
    address: "100 Education Blvd",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    contactEmail: "main@springfieldacademy.edu",
    contactPhone: "+1-217-555-0101",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[0].toString(),
    name: "North Campus",
    address: "450 North Street",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    contactEmail: "north@springfieldacademy.edu",
    contactPhone: "+1-217-555-0102",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Riverside College — 2 branches
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[1].toString(),
    name: "Downtown Campus",
    address: "250 River Road",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    contactEmail: "downtown@riversidecollege.edu",
    contactPhone: "+1-512-555-0201",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[1].toString(),
    name: "East Campus",
    address: "800 East Highway",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    contactEmail: "east@riversidecollege.edu",
    contactPhone: "+1-512-555-0202",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Oakwood International — 2 branches
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[2].toString(),
    name: "Central Branch",
    address: "500 Oak Avenue",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    contactEmail: "central@oakwoodintl.edu",
    contactPhone: "+1-503-555-0301",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    arkId: "ARK-BR-" + nanoid(),
    organizationId: orgIds[2].toString(),
    name: "West Branch",
    address: "900 West Park Drive",
    city: "Portland",
    state: "OR",
    zipCode: "97209",
    contactEmail: "west@oakwoodintl.edu",
    contactPhone: "+1-503-555-0302",
    status: "INACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

const branchIds = Object.values(branches.insertedIds);
print(`Inserted ${branchIds.length} branches`);

// =============================================================
// Academic Classes (10 classes across branches)
// =============================================================
print("--- Inserting Academic Classes ---");

const classes = db.academic_classes.insertMany([
  // Springfield Main Campus (3 classes)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    name: "Grade 10",
    section: "A",
    academicYear: "2025-2026",
    capacity: 40,
    description: "10th Grade Section A",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    name: "Grade 10",
    section: "B",
    academicYear: "2025-2026",
    capacity: 40,
    description: "10th Grade Section B",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    name: "Grade 11",
    section: "A",
    academicYear: "2025-2026",
    capacity: 35,
    description: "11th Grade Section A - Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Springfield North Campus (2 classes)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    name: "Grade 9",
    section: "A",
    academicYear: "2025-2026",
    capacity: 45,
    description: "9th Grade Section A",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    name: "Grade 9",
    section: "B",
    academicYear: "2025-2026",
    capacity: 45,
    description: "9th Grade Section B",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Riverside Downtown Campus (2 classes)
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    name: "Computer Science 101",
    section: null,
    academicYear: "2025-2026",
    capacity: 60,
    description: "Introduction to Computer Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    name: "Mathematics 201",
    section: null,
    academicYear: "2025-2026",
    capacity: 50,
    description: "Advanced Mathematics",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Riverside East Campus (1 class)
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[3].toString(),
    name: "Physics 101",
    section: null,
    academicYear: "2025-2026",
    capacity: 40,
    description: "Introduction to Physics",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Oakwood Central Branch (2 classes)
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    name: "Year 8",
    section: "Alpha",
    academicYear: "2025-2026",
    capacity: 30,
    description: "Year 8 Alpha Section",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    name: "Year 8",
    section: "Beta",
    academicYear: "2025-2026",
    capacity: 30,
    description: "Year 8 Beta Section",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

const classIds = Object.values(classes.insertedIds);
print(`Inserted ${classIds.length} academic classes`);

// =============================================================
// Students (20 students across all orgs with varied statuses)
// =============================================================
print("--- Inserting Students ---");

const students = db.students.insertMany([
  // ---- Springfield Main Campus ----
  // Grade 10A
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-001",
    firstName: "Emma",
    lastName: "Johnson",
    email: "emma.johnson@student.springfieldacademy.edu",
    phone: "+1-217-555-1001",
    dateOfBirth: new Date("2010-03-15"),
    gender: "Female",
    address: "12 Maple Street",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    guardianName: "Robert Johnson",
    guardianPhone: "+1-217-555-1002",
    guardianEmail: "robert.johnson@email.com",
    enrollmentDate: new Date("2024-08-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-002",
    firstName: "Liam",
    lastName: "Williams",
    email: "liam.williams@student.springfieldacademy.edu",
    phone: "+1-217-555-1003",
    dateOfBirth: new Date("2010-07-22"),
    gender: "Male",
    address: "45 Oak Lane",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    guardianName: "Sarah Williams",
    guardianPhone: "+1-217-555-1004",
    guardianEmail: "sarah.williams@email.com",
    enrollmentDate: new Date("2024-08-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-003",
    firstName: "Aiden",
    lastName: "Clark",
    email: "aiden.clark@student.springfieldacademy.edu",
    phone: "+1-217-555-1020",
    dateOfBirth: new Date("2010-09-11"),
    gender: "Male",
    address: "89 Birch Court",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    guardianName: "Kevin Clark",
    guardianPhone: "+1-217-555-1021",
    guardianEmail: "kevin.clark@email.com",
    enrollmentDate: new Date("2024-08-15"),
    status: "INACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Grade 10B
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-004",
    firstName: "Olivia",
    lastName: "Brown",
    email: "olivia.brown@student.springfieldacademy.edu",
    phone: "+1-217-555-1005",
    dateOfBirth: new Date("2010-11-08"),
    gender: "Female",
    address: "78 Elm Drive",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    guardianName: "James Brown",
    guardianPhone: "+1-217-555-1006",
    guardianEmail: "james.brown@email.com",
    enrollmentDate: new Date("2024-08-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Grade 11A
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[2].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-005",
    firstName: "Noah",
    lastName: "Davis",
    email: "noah.davis@student.springfieldacademy.edu",
    phone: "+1-217-555-1007",
    dateOfBirth: new Date("2009-05-30"),
    gender: "Male",
    address: "123 Pine Road",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    guardianName: "Michael Davis",
    guardianPhone: "+1-217-555-1008",
    guardianEmail: "michael.davis@email.com",
    enrollmentDate: new Date("2023-08-20"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[2].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-006",
    firstName: "Charlotte",
    lastName: "Lee",
    email: "charlotte.lee@student.springfieldacademy.edu",
    phone: "+1-217-555-1022",
    dateOfBirth: new Date("2009-02-14"),
    gender: "Female",
    address: "55 Willow Way",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    guardianName: "Daniel Lee",
    guardianPhone: "+1-217-555-1023",
    guardianEmail: "daniel.lee@email.com",
    enrollmentDate: new Date("2023-08-20"),
    status: "GRADUATED",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Springfield North Campus ----
  // Grade 9A
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    classId: classIds[3].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-007",
    firstName: "Sophia",
    lastName: "Martinez",
    email: "sophia.martinez@student.springfieldacademy.edu",
    phone: "+1-217-555-1009",
    dateOfBirth: new Date("2011-01-12"),
    gender: "Female",
    address: "456 North Avenue",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    guardianName: "Carlos Martinez",
    guardianPhone: "+1-217-555-1010",
    guardianEmail: "carlos.martinez@email.com",
    enrollmentDate: new Date("2025-08-18"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    classId: classIds[3].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-008",
    firstName: "Jackson",
    lastName: "White",
    email: "jackson.white@student.springfieldacademy.edu",
    phone: "+1-217-555-1024",
    dateOfBirth: new Date("2011-06-03"),
    gender: "Male",
    address: "77 Cedar Lane",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    guardianName: "Karen White",
    guardianPhone: "+1-217-555-1025",
    guardianEmail: "karen.white@email.com",
    enrollmentDate: new Date("2025-08-18"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Grade 9B
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    classId: classIds[4].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "SA-2026-009",
    firstName: "Harper",
    lastName: "Young",
    email: "harper.young@student.springfieldacademy.edu",
    phone: "+1-217-555-1026",
    dateOfBirth: new Date("2011-10-19"),
    gender: "Female",
    address: "200 Spruce Street",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    guardianName: "Andrew Young",
    guardianPhone: "+1-217-555-1027",
    guardianEmail: "andrew.young@email.com",
    enrollmentDate: new Date("2025-08-18"),
    status: "TRANSFERRED",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Riverside Downtown Campus ----
  // CS 101
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-001",
    firstName: "Ethan",
    lastName: "Garcia",
    email: "ethan.garcia@student.riversidecollege.edu",
    phone: "+1-512-555-2001",
    dateOfBirth: new Date("2005-09-14"),
    gender: "Male",
    address: "789 River Lane",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    guardianName: "Maria Garcia",
    guardianPhone: "+1-512-555-2002",
    guardianEmail: "maria.garcia@email.com",
    enrollmentDate: new Date("2025-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-002",
    firstName: "Ava",
    lastName: "Wilson",
    email: "ava.wilson@student.riversidecollege.edu",
    phone: "+1-512-555-2003",
    dateOfBirth: new Date("2005-04-25"),
    gender: "Female",
    address: "321 College Street",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    guardianName: "David Wilson",
    guardianPhone: "+1-512-555-2004",
    guardianEmail: "david.wilson@email.com",
    enrollmentDate: new Date("2025-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-003",
    firstName: "Ryan",
    lastName: "Patel",
    email: "ryan.patel@student.riversidecollege.edu",
    phone: "+1-512-555-2010",
    dateOfBirth: new Date("2005-01-08"),
    gender: "Male",
    address: "444 Tech Blvd",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    guardianName: "Priya Patel",
    guardianPhone: "+1-512-555-2011",
    guardianEmail: "priya.patel@email.com",
    enrollmentDate: new Date("2025-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Math 201
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[6].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-004",
    firstName: "Mason",
    lastName: "Taylor",
    email: "mason.taylor@student.riversidecollege.edu",
    phone: "+1-512-555-2005",
    dateOfBirth: new Date("2004-12-03"),
    gender: "Male",
    address: "654 University Blvd",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    guardianName: "Jennifer Taylor",
    guardianPhone: "+1-512-555-2006",
    guardianEmail: "jennifer.taylor@email.com",
    enrollmentDate: new Date("2024-08-20"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[6].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-005",
    firstName: "Zoe",
    lastName: "Kim",
    email: "zoe.kim@student.riversidecollege.edu",
    phone: "+1-512-555-2012",
    dateOfBirth: new Date("2005-03-17"),
    gender: "Female",
    address: "999 Algebra Ave",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    guardianName: "Min-Jun Kim",
    guardianPhone: "+1-512-555-2013",
    guardianEmail: "minjun.kim@email.com",
    enrollmentDate: new Date("2024-08-20"),
    status: "DROPPED",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Riverside East Campus ----
  // Physics 101
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[3].toString(),
    classId: classIds[7].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-006",
    firstName: "Ella",
    lastName: "Hernandez",
    email: "ella.hernandez@student.riversidecollege.edu",
    phone: "+1-512-555-2014",
    dateOfBirth: new Date("2005-08-21"),
    gender: "Female",
    address: "111 East View",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    guardianName: "Luis Hernandez",
    guardianPhone: "+1-512-555-2015",
    guardianEmail: "luis.hernandez@email.com",
    enrollmentDate: new Date("2025-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[3].toString(),
    classId: classIds[7].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "RC-2026-007",
    firstName: "James",
    lastName: "Moore",
    email: "james.moore@student.riversidecollege.edu",
    phone: "+1-512-555-2016",
    dateOfBirth: new Date("2004-11-09"),
    gender: "Male",
    address: "222 Physics Lane",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    guardianName: "Sandra Moore",
    guardianPhone: "+1-512-555-2017",
    guardianEmail: "sandra.moore@email.com",
    enrollmentDate: new Date("2025-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Oakwood Central Branch ----
  // Year 8 Alpha
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[8].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "OW-2026-001",
    firstName: "Isabella",
    lastName: "Anderson",
    email: "isabella.anderson@student.oakwoodintl.edu",
    phone: "+1-503-555-3001",
    dateOfBirth: new Date("2012-06-18"),
    gender: "Female",
    address: "900 Oak Terrace",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    guardianName: "Thomas Anderson",
    guardianPhone: "+1-503-555-3002",
    guardianEmail: "thomas.anderson@email.com",
    enrollmentDate: new Date("2025-09-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[8].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "OW-2026-002",
    firstName: "Lucas",
    lastName: "Thomas",
    email: "lucas.thomas@student.oakwoodintl.edu",
    phone: "+1-503-555-3003",
    dateOfBirth: new Date("2012-02-27"),
    gender: "Male",
    address: "111 Forest Path",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    guardianName: "Patricia Thomas",
    guardianPhone: "+1-503-555-3004",
    guardianEmail: "patricia.thomas@email.com",
    enrollmentDate: new Date("2025-09-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // Year 8 Beta
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[9].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "OW-2026-003",
    firstName: "Mia",
    lastName: "Nakamura",
    email: "mia.nakamura@student.oakwoodintl.edu",
    phone: "+1-503-555-3005",
    dateOfBirth: new Date("2012-04-10"),
    gender: "Female",
    address: "444 Sakura Lane",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    guardianName: "Hiroshi Nakamura",
    guardianPhone: "+1-503-555-3006",
    guardianEmail: "hiroshi.nakamura@email.com",
    enrollmentDate: new Date("2025-09-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[9].toString(),
    arkId: "ARK-STU-" + nanoid(),
    rollNumber: "OW-2026-004",
    firstName: "Oscar",
    lastName: "Fernandez",
    email: "oscar.fernandez@student.oakwoodintl.edu",
    phone: "+1-503-555-3007",
    dateOfBirth: new Date("2012-08-30"),
    gender: "Male",
    address: "555 Globe Street",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    guardianName: "Rosa Fernandez",
    guardianPhone: "+1-503-555-3008",
    guardianEmail: "rosa.fernandez@email.com",
    enrollmentDate: new Date("2025-09-01"),
    status: "INACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

const studentIds = Object.values(students.insertedIds);
print(`Inserted ${studentIds.length} students`);

// =============================================================
// Faculty (12 faculty across all orgs with varied statuses)
// =============================================================
print("--- Inserting Faculty ---");

const facultyResult = db.faculty.insertMany([
  // ---- Springfield Academy ----
  // Main Campus (3)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "SA-FAC-001",
    firstName: "Dr. Margaret",
    lastName: "Chen",
    email: "margaret.chen@springfieldacademy.edu",
    phone: "+1-217-555-5001",
    dateOfBirth: new Date("1978-04-10"),
    gender: "Female",
    address: "200 Faculty Lane",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    department: "Science",
    designation: "Head of Department",
    qualifications: ["Ph.D. Physics", "M.Sc. Physics", "B.Sc. Physics"],
    specializations: ["Quantum Mechanics", "Thermodynamics"],
    joiningDate: new Date("2015-06-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "SA-FAC-002",
    firstName: "James",
    lastName: "Patterson",
    email: "james.patterson@springfieldacademy.edu",
    phone: "+1-217-555-5002",
    dateOfBirth: new Date("1985-09-22"),
    gender: "Male",
    address: "305 Teacher Court",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    department: "Mathematics",
    designation: "Senior Teacher",
    qualifications: ["M.Sc. Mathematics", "B.Ed."],
    specializations: ["Calculus", "Statistics"],
    joiningDate: new Date("2018-08-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "SA-FAC-003",
    firstName: "Rachel",
    lastName: "Adams",
    email: "rachel.adams@springfieldacademy.edu",
    phone: "+1-217-555-5010",
    dateOfBirth: new Date("1992-03-18"),
    gender: "Female",
    address: "150 Academic Way",
    city: "Springfield",
    state: "IL",
    zipCode: "62704",
    department: "English",
    designation: "Teacher",
    qualifications: ["M.A. English", "B.Ed."],
    specializations: ["Grammar", "Literature"],
    joiningDate: new Date("2022-07-01"),
    status: "ON_LEAVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // North Campus (2)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "SA-FAC-004",
    firstName: "Linda",
    lastName: "Thompson",
    email: "linda.thompson@springfieldacademy.edu",
    phone: "+1-217-555-5003",
    dateOfBirth: new Date("1990-01-15"),
    gender: "Female",
    address: "410 North Faculty Road",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    department: "English",
    designation: "Teacher",
    qualifications: ["M.A. English Literature", "B.A. English"],
    specializations: ["Creative Writing", "Modern Literature"],
    joiningDate: new Date("2021-07-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "SA-FAC-005",
    firstName: "David",
    lastName: "Nguyen",
    email: "david.nguyen@springfieldacademy.edu",
    phone: "+1-217-555-5011",
    dateOfBirth: new Date("1987-11-25"),
    gender: "Male",
    address: "620 North Scholar Blvd",
    city: "Springfield",
    state: "IL",
    zipCode: "62707",
    department: "Science",
    designation: "Senior Teacher",
    qualifications: ["M.Sc. Chemistry", "B.Sc. Chemistry"],
    specializations: ["Organic Chemistry", "Biochemistry"],
    joiningDate: new Date("2019-08-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Riverside College ----
  // Downtown Campus (3)
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "RC-FAC-001",
    firstName: "Prof. Alan",
    lastName: "Turing",
    email: "alan.turing@riversidecollege.edu",
    phone: "+1-512-555-6001",
    dateOfBirth: new Date("1975-11-05"),
    gender: "Male",
    address: "600 Campus Drive",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    department: "Computer Science",
    designation: "Professor",
    qualifications: ["Ph.D. Computer Science", "M.S. Computer Science", "B.Tech. IT"],
    specializations: ["Artificial Intelligence", "Machine Learning", "Algorithms"],
    joiningDate: new Date("2010-01-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "RC-FAC-002",
    firstName: "Dr. Emily",
    lastName: "Noether",
    email: "emily.noether@riversidecollege.edu",
    phone: "+1-512-555-6002",
    dateOfBirth: new Date("1982-07-18"),
    gender: "Female",
    address: "750 Math Hall",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    department: "Mathematics",
    designation: "Associate Professor",
    qualifications: ["Ph.D. Mathematics", "M.Sc. Applied Mathematics"],
    specializations: ["Abstract Algebra", "Number Theory"],
    joiningDate: new Date("2014-08-20"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "RC-FAC-003",
    firstName: "Sarah",
    lastName: "Blake",
    email: "sarah.blake@riversidecollege.edu",
    phone: "+1-512-555-6010",
    dateOfBirth: new Date("1990-05-22"),
    gender: "Female",
    address: "450 Research Park",
    city: "Austin",
    state: "TX",
    zipCode: "73301",
    department: "Computer Science",
    designation: "Assistant Professor",
    qualifications: ["Ph.D. Computer Science", "M.S. Data Science"],
    specializations: ["Data Engineering", "Database Systems"],
    joiningDate: new Date("2023-01-15"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // East Campus (2)
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[3].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "RC-FAC-004",
    firstName: "Mark",
    lastName: "Rivera",
    email: "mark.rivera@riversidecollege.edu",
    phone: "+1-512-555-6003",
    dateOfBirth: new Date("1988-03-30"),
    gender: "Male",
    address: "888 East Campus Rd",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    department: "Physics",
    designation: "Assistant Professor",
    qualifications: ["M.S. Physics", "B.S. Engineering Physics"],
    specializations: ["Classical Mechanics", "Electromagnetism"],
    joiningDate: new Date("2022-01-10"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[1].toString(),
    branchId: branchIds[3].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "RC-FAC-005",
    firstName: "Nancy",
    lastName: "Cooper",
    email: "nancy.cooper@riversidecollege.edu",
    phone: "+1-512-555-6011",
    dateOfBirth: new Date("1984-09-14"),
    gender: "Female",
    address: "333 East Faculty Row",
    city: "Austin",
    state: "TX",
    zipCode: "73344",
    department: "Mathematics",
    designation: "Senior Lecturer",
    qualifications: ["M.Sc. Mathematics", "B.Sc. Mathematics"],
    specializations: ["Linear Algebra", "Probability"],
    joiningDate: new Date("2017-08-01"),
    status: "RESIGNED",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Oakwood International ----
  // Central Branch (2)
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "OW-FAC-001",
    firstName: "Dr. Kenji",
    lastName: "Tanaka",
    email: "kenji.tanaka@oakwoodintl.edu",
    phone: "+1-503-555-7001",
    dateOfBirth: new Date("1980-08-12"),
    gender: "Male",
    address: "222 International Way",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    department: "Science",
    designation: "Head of Department",
    qualifications: ["Ph.D. Biology", "M.Sc. Molecular Biology"],
    specializations: ["Genetics", "Biotechnology"],
    joiningDate: new Date("2016-04-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    arkId: "ARK-FAC-" + nanoid(),
    employeeId: "OW-FAC-002",
    firstName: "Amara",
    lastName: "Okafor",
    email: "amara.okafor@oakwoodintl.edu",
    phone: "+1-503-555-7002",
    dateOfBirth: new Date("1992-12-05"),
    gender: "Female",
    address: "333 Oak Meadow",
    city: "Portland",
    state: "OR",
    zipCode: "97201",
    department: "Mathematics",
    designation: "Teacher",
    qualifications: ["M.Sc. Mathematics", "B.Ed."],
    specializations: ["Geometry", "Algebra"],
    joiningDate: new Date("2023-08-01"),
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

print(`Inserted ${facultyResult.insertedIds ? Object.keys(facultyResult.insertedIds).length : 0} faculty members`);

// =============================================================
// Users (11 users across all orgs — all roles represented)
// Password for all: Password@123
//
// Role mapping rules:
//   SUPER_ADMIN — no orgId, no branchId
//   ORG_ADMIN   — orgId required, branchId optional
//   ADMIN       — orgId required, branchId REQUIRED
//   USER        — orgId required, branchId REQUIRED
// =============================================================
print("--- Inserting Users ---");

const usersResult = db.users.insertMany([
  // 1. Super Admin (platform-wide, no org/branch)
  {
    fullName: "Platform Super Admin",
    email: "superadmin@ark-platform.com",
    password: bcryptHash,
    role: "SUPER_ADMIN",
    organizationId: null,
    branchId: null,
    department: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Springfield Academy users ----
  // 2. Org Admin (org-level, no branch)
  {
    fullName: "John Springfield",
    email: "orgadmin@springfieldacademy.edu",
    password: bcryptHash,
    role: "ORG_ADMIN",
    organizationId: orgIds[0].toString(),
    branchId: null,
    department: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 3. Admin — Main Campus, Science
  {
    fullName: "Sarah Records",
    email: "admin@springfieldacademy.edu",
    password: bcryptHash,
    role: "ADMIN",
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    department: "Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 4. Admin — North Campus, English
  {
    fullName: "Peter North",
    email: "admin.north@springfieldacademy.edu",
    password: bcryptHash,
    role: "ADMIN",
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    department: "English",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 5. User — Main Campus, Science
  {
    fullName: "Mike Viewer",
    email: "user@springfieldacademy.edu",
    password: bcryptHash,
    role: "USER",
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    department: "Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Riverside College users ----
  // 6. Org Admin (org-level, no branch)
  {
    fullName: "Diana Riverside",
    email: "orgadmin@riversidecollege.edu",
    password: bcryptHash,
    role: "ORG_ADMIN",
    organizationId: orgIds[1].toString(),
    branchId: null,
    department: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 7. Admin — Downtown Campus, Computer Science
  {
    fullName: "Tom Academic",
    email: "admin@riversidecollege.edu",
    password: bcryptHash,
    role: "ADMIN",
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    department: "Computer Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 8. User — Downtown Campus, Mathematics
  {
    fullName: "Lisa Reader",
    email: "user@riversidecollege.edu",
    password: bcryptHash,
    role: "USER",
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    department: "Mathematics",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // ---- Oakwood International users ----
  // 9. Org Admin (org-level, no branch)
  {
    fullName: "Kenneth Oakwood",
    email: "orgadmin@oakwoodintl.edu",
    password: bcryptHash,
    role: "ORG_ADMIN",
    organizationId: orgIds[2].toString(),
    branchId: null,
    department: null,
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 10. Admin — Central Branch, Science
  {
    fullName: "Yuki Tanaka",
    email: "admin@oakwoodintl.edu",
    password: bcryptHash,
    role: "ADMIN",
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    department: "Science",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  // 11. User — Central Branch, Mathematics
  {
    fullName: "Priya Sharma",
    email: "user@oakwoodintl.edu",
    password: bcryptHash,
    role: "USER",
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    department: "Mathematics",
    status: "ACTIVE",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

const userIds = Object.values(usersResult.insertedIds);
print(`Inserted ${userIds.length} users`);

// =============================================================
// Audit Logs (seed data for dashboard graphs)
// =============================================================
print("--- Inserting Audit Logs ---");

const now = new Date();
const oneHourAgo = new Date(now.getTime() - 3600000);
const twoHoursAgo = new Date(now.getTime() - 7200000);
const oneDayAgo = new Date(now.getTime() - 86400000);
const twoDaysAgo = new Date(now.getTime() - 172800000);
const threeDaysAgo = new Date(now.getTime() - 259200000);

const auditLogs = db.audit_logs.insertMany([
  // Platform-level actions by Super Admin
  {
    action: "CREATE",
    entityType: "Organization",
    entityId: orgIds[0].toString(),
    entityName: "Springfield Academy",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[0].toString(),
    performedByEmail: "superadmin@ark-platform.com",
    performedByRole: "SUPER_ADMIN",
    details: "Organization created",
    timestamp: threeDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Organization",
    entityId: orgIds[1].toString(),
    entityName: "Riverside College",
    organizationId: orgIds[1].toString(),
    performedBy: userIds[0].toString(),
    performedByEmail: "superadmin@ark-platform.com",
    performedByRole: "SUPER_ADMIN",
    details: "Organization created",
    timestamp: threeDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Organization",
    entityId: orgIds[2].toString(),
    entityName: "Oakwood International School",
    organizationId: orgIds[2].toString(),
    performedBy: userIds[0].toString(),
    performedByEmail: "superadmin@ark-platform.com",
    performedByRole: "SUPER_ADMIN",
    details: "Organization created",
    timestamp: threeDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  // Login actions
  {
    action: "LOGIN",
    entityType: "User",
    entityId: userIds[0].toString(),
    entityName: null,
    organizationId: null,
    performedBy: userIds[0].toString(),
    performedByEmail: "superadmin@ark-platform.com",
    performedByRole: "SUPER_ADMIN",
    details: "User logged in",
    timestamp: twoHoursAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "LOGIN",
    entityType: "User",
    entityId: userIds[1].toString(),
    entityName: null,
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "User logged in",
    timestamp: oneHourAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "LOGIN",
    entityType: "User",
    entityId: userIds[5].toString(),
    entityName: null,
    organizationId: orgIds[1].toString(),
    performedBy: userIds[5].toString(),
    performedByEmail: "orgadmin@riversidecollege.edu",
    performedByRole: "ORG_ADMIN",
    details: "User logged in",
    timestamp: oneHourAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  // Springfield — branch, class, student, faculty actions
  {
    action: "CREATE",
    entityType: "Branch",
    entityId: branchIds[0].toString(),
    entityName: "Main Campus",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "Branch created",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Branch",
    entityId: branchIds[1].toString(),
    entityName: "North Campus",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "Branch created",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Student",
    entityId: "seed-student-1",
    entityName: "Emma Johnson",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[2].toString(),
    performedByEmail: "admin@springfieldacademy.edu",
    performedByRole: "ADMIN",
    details: "Student created: SA-2026-001",
    timestamp: oneDayAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Student",
    entityId: "seed-student-2",
    entityName: "Liam Williams",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[2].toString(),
    performedByEmail: "admin@springfieldacademy.edu",
    performedByRole: "ADMIN",
    details: "Student created: SA-2026-002",
    timestamp: oneDayAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "UPDATE",
    entityType: "Student",
    entityId: "seed-student-1",
    entityName: "Emma Johnson",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[2].toString(),
    performedByEmail: "admin@springfieldacademy.edu",
    performedByRole: "ADMIN",
    details: "Student updated",
    timestamp: oneHourAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Faculty",
    entityId: "seed-faculty-1",
    entityName: "Dr. Margaret Chen",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "Faculty created: SA-FAC-001",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "User",
    entityId: userIds[2].toString(),
    entityName: "Sarah Records",
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "User created with role ADMIN",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  // Riverside — actions
  {
    action: "CREATE",
    entityType: "Branch",
    entityId: branchIds[2].toString(),
    entityName: "Downtown Campus",
    organizationId: orgIds[1].toString(),
    performedBy: userIds[5].toString(),
    performedByEmail: "orgadmin@riversidecollege.edu",
    performedByRole: "ORG_ADMIN",
    details: "Branch created",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Student",
    entityId: "seed-student-rc1",
    entityName: "Ethan Garcia",
    organizationId: orgIds[1].toString(),
    performedBy: userIds[6].toString(),
    performedByEmail: "admin@riversidecollege.edu",
    performedByRole: "ADMIN",
    details: "Student created: RC-2026-001",
    timestamp: oneDayAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Faculty",
    entityId: "seed-faculty-rc1",
    entityName: "Prof. Alan Turing",
    organizationId: orgIds[1].toString(),
    performedBy: userIds[5].toString(),
    performedByEmail: "orgadmin@riversidecollege.edu",
    performedByRole: "ORG_ADMIN",
    details: "Faculty created: RC-FAC-001",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "DELETE",
    entityType: "Student",
    entityId: "seed-student-rc-del",
    entityName: "Deleted Student",
    organizationId: orgIds[1].toString(),
    performedBy: userIds[6].toString(),
    performedByEmail: "admin@riversidecollege.edu",
    performedByRole: "ADMIN",
    details: "Student deleted: RC-2026-999",
    timestamp: oneHourAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  // Oakwood — actions
  {
    action: "CREATE",
    entityType: "Branch",
    entityId: branchIds[4].toString(),
    entityName: "Central Branch",
    organizationId: orgIds[2].toString(),
    performedBy: userIds[8].toString(),
    performedByEmail: "orgadmin@oakwoodintl.edu",
    performedByRole: "ORG_ADMIN",
    details: "Branch created",
    timestamp: twoDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "CREATE",
    entityType: "Student",
    entityId: "seed-student-ow1",
    entityName: "Isabella Anderson",
    organizationId: orgIds[2].toString(),
    performedBy: userIds[9].toString(),
    performedByEmail: "admin@oakwoodintl.edu",
    performedByRole: "ADMIN",
    details: "Student created: OW-2026-001",
    timestamp: oneDayAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "UPDATE",
    entityType: "Organization",
    entityId: orgIds[2].toString(),
    entityName: "Oakwood International School",
    organizationId: orgIds[2].toString(),
    performedBy: userIds[0].toString(),
    performedByEmail: "superadmin@ark-platform.com",
    performedByRole: "SUPER_ADMIN",
    details: "Organization updated — status changed to INACTIVE",
    timestamp: now,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  // Register actions
  {
    action: "REGISTER",
    entityType: "User",
    entityId: userIds[1].toString(),
    entityName: null,
    organizationId: orgIds[0].toString(),
    performedBy: userIds[1].toString(),
    performedByEmail: "orgadmin@springfieldacademy.edu",
    performedByRole: "ORG_ADMIN",
    details: "User registered: John Springfield",
    timestamp: threeDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  },
  {
    action: "REGISTER",
    entityType: "User",
    entityId: userIds[5].toString(),
    entityName: null,
    organizationId: orgIds[1].toString(),
    performedBy: userIds[5].toString(),
    performedByEmail: "orgadmin@riversidecollege.edu",
    performedByRole: "ORG_ADMIN",
    details: "User registered: Diana Riverside",
    timestamp: threeDaysAgo,
    _class: "com.app.ark_backend_services.model.AuditLog"
  }
]);

print(`Inserted ${auditLogs.insertedIds ? Object.keys(auditLogs.insertedIds).length : 0} audit logs`);

// =============================================================
// Examinations (4 exams across orgs/branches)
// =============================================================
print("--- Inserting Examinations ---");

const exams = db.examinations.insertMany([
  // Springfield Main Campus — Mid-Term (COMPLETED) and Final (SCHEDULED)
  {
    arkId: "ARK-EXM-" + nanoid(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    name: "Mid-Term Examination",
    academicYear: "2025-2026",
    examType: "MIDTERM",
    startDate: new Date("2025-10-01"),
    endDate: new Date("2025-10-15"),
    description: "Mid-term examination for all classes",
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.Examination"
  },
  {
    arkId: "ARK-EXM-" + nanoid(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    name: "Final Examination",
    academicYear: "2025-2026",
    examType: "FINAL",
    startDate: new Date("2026-03-15"),
    endDate: new Date("2026-03-30"),
    description: "Annual final examination",
    status: "SCHEDULED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.Examination"
  },
  // Riverside Downtown Campus — Quarterly (COMPLETED)
  {
    arkId: "ARK-EXM-" + nanoid(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    name: "Quarterly Assessment Q1",
    academicYear: "2025-2026",
    examType: "QUARTERLY",
    startDate: new Date("2025-09-15"),
    endDate: new Date("2025-09-20"),
    description: "First quarter assessment",
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.Examination"
  },
  // Oakwood Central Branch — Unit Test (IN_PROGRESS)
  {
    arkId: "ARK-EXM-" + nanoid(),
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    name: "Unit Test 1",
    academicYear: "2025-2026",
    examType: "UNIT_TEST",
    startDate: new Date("2026-03-05"),
    endDate: new Date("2026-03-10"),
    description: "First unit test of the year",
    status: "IN_PROGRESS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.Examination"
  }
]);

const examIds = Object.values(exams.insertedIds);
print(`Inserted ${examIds.length} examinations`);

// =============================================================
// Exam Subjects (subjects for the completed exams)
// =============================================================
print("--- Inserting Exam Subjects ---");

// Springfield Mid-Term: subjects for Grade 10A (classIds[0]) and Grade 10B (classIds[1])
// Riverside Quarterly: subjects for CS 101 (classIds[5]) and Math 201 (classIds[6])
// Oakwood Unit Test: subjects for Year 8 Alpha (classIds[8])
const examSubjects = db.exam_subjects.insertMany([
  // Springfield Mid-Term — Grade 10A subjects
  {
    examinationId: examIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    subjectName: "Mathematics",
    subjectCode: "MATH-10",
    maxMarks: 100,
    passingMarks: 33,
    examDate: new Date("2025-10-01"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  {
    examinationId: examIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    subjectName: "English",
    subjectCode: "ENG-10",
    maxMarks: 100,
    passingMarks: 33,
    examDate: new Date("2025-10-03"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  {
    examinationId: examIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    subjectName: "Science",
    subjectCode: "SCI-10",
    maxMarks: 100,
    passingMarks: 33,
    examDate: new Date("2025-10-05"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  // Springfield Mid-Term — Grade 10B subjects
  {
    examinationId: examIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    subjectName: "Mathematics",
    subjectCode: "MATH-10",
    maxMarks: 100,
    passingMarks: 33,
    examDate: new Date("2025-10-01"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  {
    examinationId: examIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    subjectName: "English",
    subjectCode: "ENG-10",
    maxMarks: 100,
    passingMarks: 33,
    examDate: new Date("2025-10-03"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  // Riverside Quarterly — CS 101 subjects
  {
    examinationId: examIds[2].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    subjectName: "Programming Fundamentals",
    subjectCode: "CS-101-P",
    maxMarks: 50,
    passingMarks: 20,
    examDate: new Date("2025-09-15"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  {
    examinationId: examIds[2].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    subjectName: "Data Structures",
    subjectCode: "CS-101-D",
    maxMarks: 50,
    passingMarks: 20,
    examDate: new Date("2025-09-17"),
    status: "COMPLETED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  // Oakwood Unit Test — Year 8 Alpha subjects
  {
    examinationId: examIds[3].toString(),
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[8].toString(),
    subjectName: "Mathematics",
    subjectCode: "MATH-Y8",
    maxMarks: 50,
    passingMarks: 17,
    examDate: new Date("2026-03-05"),
    status: "SCHEDULED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  },
  {
    examinationId: examIds[3].toString(),
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    classId: classIds[8].toString(),
    subjectName: "English",
    subjectCode: "ENG-Y8",
    maxMarks: 50,
    passingMarks: 17,
    examDate: new Date("2026-03-07"),
    status: "SCHEDULED",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamSubject"
  }
]);

const subjectIds = Object.values(examSubjects.insertedIds);
print(`Inserted ${subjectIds.length} exam subjects`);

// =============================================================
// Exam Results (student scores for completed exams)
// =============================================================
print("--- Inserting Exam Results ---");

// Grade calculation helper
function calcGrade(marks, max) {
  const pct = (marks / max) * 100;
  if (pct >= 90) return "A+";
  if (pct >= 80) return "A";
  if (pct >= 70) return "B+";
  if (pct >= 60) return "B";
  if (pct >= 50) return "C";
  if (pct >= 40) return "D";
  return "F";
}

// Students in Grade 10A (Springfield Main): studentIds[0]=Emma, studentIds[1]=Liam, studentIds[2]=Olivia
// Students in Grade 10B (Springfield Main): studentIds[3]=Noah, studentIds[4]=Ava
// Students in CS 101 (Riverside Downtown): studentIds[8]=Sophia, studentIds[9]=Benjamin
// subjectIds[0]=Math 10A, [1]=Eng 10A, [2]=Sci 10A, [3]=Math 10B, [4]=Eng 10B
// subjectIds[5]=Prog Fund CS101, [6]=DS CS101

const examResults = db.exam_results.insertMany([
  // === Springfield Mid-Term — Grade 10A ===
  // Emma (studentIds[0]) — strong student
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[0].toString(),
    marksObtained: 92,
    grade: calcGrade(92, 100),
    remarks: "Excellent performance",
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[1].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[0].toString(),
    marksObtained: 88,
    grade: calcGrade(88, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[2].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[0].toString(),
    marksObtained: 95,
    grade: calcGrade(95, 100),
    remarks: "Top scorer in Science",
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // Liam (studentIds[1]) — average student
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[1].toString(),
    marksObtained: 65,
    grade: calcGrade(65, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[1].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[1].toString(),
    marksObtained: 72,
    grade: calcGrade(72, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[2].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[1].toString(),
    marksObtained: 58,
    grade: calcGrade(58, 100),
    remarks: "Needs improvement in practicals",
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // Olivia (studentIds[2]) — failed one subject
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[0].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[2].toString(),
    marksObtained: 45,
    grade: calcGrade(45, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[1].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[2].toString(),
    marksObtained: 28,
    grade: calcGrade(28, 100),
    remarks: "Below passing marks",
    status: "FAIL",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[2].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[0].toString(),
    studentId: studentIds[2].toString(),
    marksObtained: 55,
    grade: calcGrade(55, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // === Springfield Mid-Term — Grade 10B ===
  // Noah (studentIds[3]) — good student
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[3].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    studentId: studentIds[3].toString(),
    marksObtained: 78,
    grade: calcGrade(78, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[4].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    studentId: studentIds[3].toString(),
    marksObtained: 82,
    grade: calcGrade(82, 100),
    remarks: "Strong essay writing",
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // Ava (studentIds[4]) — absent in one subject
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[3].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    studentId: studentIds[4].toString(),
    marksObtained: 71,
    grade: calcGrade(71, 100),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[0].toString(),
    examSubjectId: subjectIds[4].toString(),
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    classId: classIds[1].toString(),
    studentId: studentIds[4].toString(),
    marksObtained: 0,
    grade: "F",
    remarks: "Absent due to medical reasons",
    status: "ABSENT",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // === Riverside Quarterly — CS 101 ===
  // Sophia (studentIds[8]) — excellent
  {
    examinationId: examIds[2].toString(),
    examSubjectId: subjectIds[5].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    studentId: studentIds[8].toString(),
    marksObtained: 47,
    grade: calcGrade(47, 50),
    remarks: "Outstanding coding skills",
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[2].toString(),
    examSubjectId: subjectIds[6].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    studentId: studentIds[8].toString(),
    marksObtained: 42,
    grade: calcGrade(42, 50),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  // Benjamin (studentIds[9]) — struggling
  {
    examinationId: examIds[2].toString(),
    examSubjectId: subjectIds[5].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    studentId: studentIds[9].toString(),
    marksObtained: 22,
    grade: calcGrade(22, 50),
    remarks: null,
    status: "PASS",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  },
  {
    examinationId: examIds[2].toString(),
    examSubjectId: subjectIds[6].toString(),
    organizationId: orgIds[1].toString(),
    branchId: branchIds[2].toString(),
    classId: classIds[5].toString(),
    studentId: studentIds[9].toString(),
    marksObtained: 15,
    grade: calcGrade(15, 50),
    remarks: "Below passing marks — needs remedial classes",
    status: "FAIL",
    createdAt: new Date(),
    updatedAt: new Date(),
    _class: "com.app.ark_backend_services.model.ExamResult"
  }
]);

print(`Inserted ${examResults.insertedIds ? Object.keys(examResults.insertedIds).length : 0} exam results`);

// =============================================================
// Create indexes
// =============================================================
print("--- Creating Indexes ---");

db.organizations.createIndex({ arkId: 1 }, { unique: true });
db.organizations.createIndex({ name: 1 }, { unique: true });
db.branches.createIndex({ arkId: 1 }, { unique: true });
db.branches.createIndex({ organizationId: 1, name: 1 }, { unique: true });
db.academic_classes.createIndex({ organizationId: 1, branchId: 1, name: 1, section: 1 }, { unique: true });
db.students.createIndex({ arkId: 1 }, { unique: true });
db.students.createIndex({ organizationId: 1, rollNumber: 1 }, { unique: true });
db.faculty.createIndex({ arkId: 1 }, { unique: true });
db.faculty.createIndex({ organizationId: 1, employeeId: 1 }, { unique: true });
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ organizationId: 1, email: 1 }, { unique: true });
db.audit_logs.createIndex({ organizationId: 1, timestamp: -1 });
db.audit_logs.createIndex({ entityType: 1, entityId: 1 });
db.examinations.createIndex({ arkId: 1 }, { unique: true });
db.examinations.createIndex({ organizationId: 1, branchId: 1, academicYear: 1, name: 1 }, { unique: true });
db.examinations.createIndex({ organizationId: 1, branchId: 1, academicYear: 1 });
db.exam_subjects.createIndex({ examinationId: 1, subjectName: 1, classId: 1 }, { unique: true });
db.exam_subjects.createIndex({ examinationId: 1, classId: 1 });
db.exam_results.createIndex({ examSubjectId: 1, studentId: 1 }, { unique: true });
db.exam_results.createIndex({ examinationId: 1, studentId: 1 });
db.exam_results.createIndex({ organizationId: 1, examinationId: 1 });

// =============================================================
// Summary
// =============================================================
print("");
print("=== SEED DATA COMPLETE ===");
print(`Organizations: ${orgIds.length} (2 ACTIVE, 1 INACTIVE)`);
print(`Branches:      ${branchIds.length} (5 ACTIVE, 1 INACTIVE)`);
print(`Classes:       ${classIds.length}`);
print(`Students:      20 (15 ACTIVE, 2 INACTIVE, 1 GRADUATED, 1 TRANSFERRED, 1 DROPPED)`);
print(`Faculty:       12 (10 ACTIVE, 1 ON_LEAVE, 1 RESIGNED)`);
print(`Users:         ${userIds.length} (1 SUPER_ADMIN, 3 ORG_ADMIN, 4 ADMIN, 3 USER)`);
print(`Audit Logs:    ${auditLogs.insertedIds ? Object.keys(auditLogs.insertedIds).length : 0}`);
print(`Examinations:  ${examIds.length} (1 COMPLETED, 1 SCHEDULED, 1 COMPLETED, 1 IN_PROGRESS)`);
print(`Exam Subjects: ${subjectIds.length}`);
print(`Exam Results:  ${examResults.insertedIds ? Object.keys(examResults.insertedIds).length : 0}`);
print("");
print("=== LOGIN CREDENTIALS (Password: Password@123) ===");
print("SUPER_ADMIN:  superadmin@ark-platform.com");
print("ORG_ADMIN:    orgadmin@springfieldacademy.edu | orgadmin@riversidecollege.edu | orgadmin@oakwoodintl.edu");
print("ADMIN:        admin@springfieldacademy.edu | admin.north@springfieldacademy.edu | admin@riversidecollege.edu | admin@oakwoodintl.edu");
print("USER:         user@springfieldacademy.edu | user@riversidecollege.edu | user@oakwoodintl.edu");
