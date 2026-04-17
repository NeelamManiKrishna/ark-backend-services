// =============================================================
// ARK Test Data - Comprehensive Seed Script (v3)
// Run: docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web test-data.js
// Or copy to container first:
//   docker cp src/main/resources/test-data.js mongodb:/tmp/test-data.js
//   docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web /tmp/test-data.js
//
// Password for all users: Password@123
// Covers: Organizations, Branches, Classes, Students (with govtId),
//         Faculty (with govtId), Users (4 RBAC tiers), Enrollments,
//         Faculty Assignments, Class Progressions, Examinations,
//         Exam Subjects, Exam Results, Audit Logs
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
db.student_enrollments.drop();
db.faculty_assignments.drop();
db.class_progressions.drop();

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

const now = new Date();

// =============================================================
// 1. Organizations (3 orgs)
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
    version: NumberLong(0),
    createdAt: now,
    updatedAt: now
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
    version: NumberLong(0),
    createdAt: now,
    updatedAt: now
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
    version: NumberLong(0),
    createdAt: now,
    updatedAt: now
  }
]);

const orgIds = Object.values(orgs.insertedIds);
print(`Inserted ${orgIds.length} organizations`);

// =============================================================
// 2. Branches (6 branches across 3 orgs)
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
    createdAt: now,
    updatedAt: now
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
    createdAt: now,
    updatedAt: now
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
    createdAt: now,
    updatedAt: now
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
    createdAt: now,
    updatedAt: now
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
    createdAt: now,
    updatedAt: now
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
    createdAt: now,
    updatedAt: now
  }
]);

const branchIds = Object.values(branches.insertedIds);
print(`Inserted ${branchIds.length} branches`);

// =============================================================
// 3. Academic Classes (12 classes across branches, 2 academic years)
// =============================================================
print("--- Inserting Academic Classes ---");

const classes = db.academic_classes.insertMany([
  // Springfield Main Campus — current year
  { organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), name: "1st Class", section: "A", academicYear: "2025-2026", capacity: 40, description: "1st Class Section A", status: "ACTIVE", createdAt: now, updatedAt: now },
  { organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), name: "2nd Class", section: "A", academicYear: "2025-2026", capacity: 40, description: "2nd Class Section A", status: "ACTIVE", createdAt: now, updatedAt: now },
  { organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), name: "3rd Class", section: "A", academicYear: "2025-2026", capacity: 35, description: "3rd Class Section A", status: "ACTIVE", createdAt: now, updatedAt: now },
  // Springfield Main Campus — prior year (for promotion testing)
  { organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), name: "1st Class", section: "A", academicYear: "2024-2025", capacity: 40, description: "1st Class Section A (prior year)", status: "COMPLETED", createdAt: now, updatedAt: now },
  // Springfield North Campus
  { organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(), name: "1st Class", section: "A", academicYear: "2025-2026", capacity: 45, description: "1st Class Section A - North", status: "ACTIVE", createdAt: now, updatedAt: now },
  // Riverside Downtown Campus
  { organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), name: "Computer Science 101", section: null, academicYear: "2025-2026", capacity: 60, description: "Intro to CS", status: "ACTIVE", createdAt: now, updatedAt: now },
  { organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), name: "Mathematics 201", section: null, academicYear: "2025-2026", capacity: 50, description: "Advanced Math", status: "ACTIVE", createdAt: now, updatedAt: now },
  // Riverside East Campus
  { organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(), name: "Physics 101", section: null, academicYear: "2025-2026", capacity: 40, description: "Intro to Physics", status: "ACTIVE", createdAt: now, updatedAt: now },
  // Oakwood Central Branch
  { organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), name: "Year 8", section: "Alpha", academicYear: "2025-2026", capacity: 30, description: "Year 8 Alpha", status: "ACTIVE", createdAt: now, updatedAt: now },
  { organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), name: "Year 9", section: "Alpha", academicYear: "2025-2026", capacity: 30, description: "Year 9 Alpha", status: "ACTIVE", createdAt: now, updatedAt: now },
  // Oakwood — prior year for promotion
  { organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), name: "Year 8", section: "Alpha", academicYear: "2024-2025", capacity: 30, description: "Year 8 Alpha (prior)", status: "COMPLETED", createdAt: now, updatedAt: now },
  { organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), name: "Year 9", section: "Alpha", academicYear: "2024-2025", capacity: 30, description: "Year 9 Alpha (prior)", status: "COMPLETED", createdAt: now, updatedAt: now }
]);

const classIds = Object.values(classes.insertedIds);
print(`Inserted ${classIds.length} academic classes`);

// =============================================================
// 4. Students (20 students with govtIdType/govtIdNumber, version)
// =============================================================
print("--- Inserting Students ---");

const students = db.students.insertMany([
  // Springfield Main Campus — 1st Class A (4 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-001", firstName: "Emma", lastName: "Johnson",
    email: "emma.johnson@student.springfield.edu", phone: "+1-217-555-1001",
    dateOfBirth: new Date("2016-03-15"), gender: "Female",
    address: "12 Maple Street", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Robert Johnson", guardianPhone: "+1-217-555-1002", guardianEmail: "robert.j@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "2345-6789-0123",
    enrollmentDate: new Date("2025-06-15"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-002", firstName: "Liam", lastName: "Williams",
    email: "liam.williams@student.springfield.edu", phone: "+1-217-555-1003",
    dateOfBirth: new Date("2016-07-22"), gender: "Male",
    address: "45 Oak Lane", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Sarah Williams", guardianPhone: "+1-217-555-1004", guardianEmail: "sarah.w@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "3456-7890-1234",
    enrollmentDate: new Date("2025-06-15"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-003", firstName: "Aiden", lastName: "Clark",
    email: "aiden.clark@student.springfield.edu", phone: "+1-217-555-1005",
    dateOfBirth: new Date("2016-09-11"), gender: "Male",
    address: "89 Birch Court", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Kevin Clark", guardianPhone: "+1-217-555-1006", guardianEmail: "kevin.c@email.com",
    govtIdType: "PASSPORT", govtIdNumber: "P1234567",
    enrollmentDate: new Date("2025-06-15"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-004", firstName: "Olivia", lastName: "Brown",
    email: "olivia.brown@student.springfield.edu", phone: "+1-217-555-1007",
    dateOfBirth: new Date("2016-11-08"), gender: "Female",
    address: "78 Elm Drive", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "James Brown", guardianPhone: "+1-217-555-1008", guardianEmail: "james.b@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "4567-8901-2345",
    enrollmentDate: new Date("2025-06-15"), status: "INACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Springfield Main Campus — 2nd Class A (2 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-005", firstName: "Noah", lastName: "Davis",
    email: "noah.davis@student.springfield.edu", phone: "+1-217-555-1009",
    dateOfBirth: new Date("2015-05-30"), gender: "Male",
    address: "123 Pine Road", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Michael Davis", guardianPhone: "+1-217-555-1010", guardianEmail: "michael.d@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "5678-9012-3456",
    enrollmentDate: new Date("2024-06-20"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-006", firstName: "Charlotte", lastName: "Lee",
    email: "charlotte.lee@student.springfield.edu", phone: "+1-217-555-1011",
    dateOfBirth: new Date("2015-02-14"), gender: "Female",
    address: "55 Willow Way", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Daniel Lee", guardianPhone: "+1-217-555-1012", guardianEmail: "daniel.l@email.com",
    govtIdType: "PAN", govtIdNumber: "ABCDE1234F",
    enrollmentDate: new Date("2024-06-20"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Springfield Main Campus — 3rd Class A (1 student — terminal for promotion test)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    rollNumber: "SA-007", firstName: "Sophia", lastName: "Martinez",
    email: "sophia.martinez@student.springfield.edu", phone: "+1-217-555-1013",
    dateOfBirth: new Date("2014-01-12"), gender: "Female",
    address: "456 North Avenue", city: "Springfield", state: "IL", zipCode: "62704",
    guardianName: "Carlos Martinez", guardianPhone: "+1-217-555-1014", guardianEmail: "carlos.m@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "6789-0123-4567",
    enrollmentDate: new Date("2023-06-15"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Springfield North Campus — 1st Class A (2 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(),
    rollNumber: "SA-N001", firstName: "Jackson", lastName: "White",
    email: "jackson.white@student.springfield.edu", phone: "+1-217-555-1015",
    dateOfBirth: new Date("2016-06-03"), gender: "Male",
    address: "77 Cedar Lane", city: "Springfield", state: "IL", zipCode: "62707",
    guardianName: "Karen White", guardianPhone: "+1-217-555-1016", guardianEmail: "karen.w@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "7890-1234-5678",
    enrollmentDate: new Date("2025-06-15"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(),
    rollNumber: "SA-N002", firstName: "Harper", lastName: "Young",
    email: "harper.young@student.springfield.edu", phone: "+1-217-555-1017",
    dateOfBirth: new Date("2016-10-19"), gender: "Female",
    address: "200 Spruce Street", city: "Springfield", state: "IL", zipCode: "62707",
    guardianName: "Andrew Young", guardianPhone: "+1-217-555-1018", guardianEmail: "andrew.y@email.com",
    govtIdType: "DRIVING_LICENSE", govtIdNumber: "DL-IL-98765",
    enrollmentDate: new Date("2025-06-15"), status: "TRANSFERRED", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Riverside Downtown — CS 101 (3 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    rollNumber: "RC-001", firstName: "Ethan", lastName: "Garcia",
    email: "ethan.garcia@student.riverside.edu", phone: "+1-512-555-2001",
    dateOfBirth: new Date("2005-09-14"), gender: "Male",
    address: "789 River Lane", city: "Austin", state: "TX", zipCode: "73301",
    guardianName: "Maria Garcia", guardianPhone: "+1-512-555-2002", guardianEmail: "maria.g@email.com",
    govtIdType: "PASSPORT", govtIdNumber: "US-8765432",
    enrollmentDate: new Date("2025-01-10"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    rollNumber: "RC-002", firstName: "Ava", lastName: "Wilson",
    email: "ava.wilson@student.riverside.edu", phone: "+1-512-555-2003",
    dateOfBirth: new Date("2005-04-25"), gender: "Female",
    address: "321 College Street", city: "Austin", state: "TX", zipCode: "73301",
    guardianName: "David Wilson", guardianPhone: "+1-512-555-2004", guardianEmail: "david.w@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "8901-2345-6789",
    enrollmentDate: new Date("2025-01-10"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    rollNumber: "RC-003", firstName: "Ryan", lastName: "Patel",
    email: "ryan.patel@student.riverside.edu", phone: "+1-512-555-2005",
    dateOfBirth: new Date("2005-01-08"), gender: "Male",
    address: "444 Tech Blvd", city: "Austin", state: "TX", zipCode: "73301",
    guardianName: "Priya Patel", guardianPhone: "+1-512-555-2006", guardianEmail: "priya.p@email.com",
    govtIdType: "PAN", govtIdNumber: "FGHIJ5678K",
    enrollmentDate: new Date("2025-01-10"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Riverside Downtown — Math 201 (2 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    rollNumber: "RC-004", firstName: "Mason", lastName: "Taylor",
    email: "mason.taylor@student.riverside.edu", phone: "+1-512-555-2007",
    dateOfBirth: new Date("2004-12-03"), gender: "Male",
    address: "654 University Blvd", city: "Austin", state: "TX", zipCode: "73301",
    guardianName: "Jennifer Taylor", guardianPhone: "+1-512-555-2008", guardianEmail: "jennifer.t@email.com",
    govtIdType: "VOTER_ID", govtIdNumber: "VOTER-TX-12345",
    enrollmentDate: new Date("2024-08-20"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    rollNumber: "RC-005", firstName: "Zoe", lastName: "Kim",
    email: "zoe.kim@student.riverside.edu", phone: "+1-512-555-2009",
    dateOfBirth: new Date("2005-03-17"), gender: "Female",
    address: "999 Algebra Ave", city: "Austin", state: "TX", zipCode: "73301",
    guardianName: "Min-Jun Kim", guardianPhone: "+1-512-555-2010", guardianEmail: "minjun.k@email.com",
    govtIdType: "PASSPORT", govtIdNumber: "KR-1234567",
    enrollmentDate: new Date("2024-08-20"), status: "DROPPED", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Riverside East Campus — Physics 101 (2 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(),
    rollNumber: "RC-E001", firstName: "Ella", lastName: "Hernandez",
    email: "ella.hernandez@student.riverside.edu", phone: "+1-512-555-2011",
    dateOfBirth: new Date("2005-08-21"), gender: "Female",
    address: "111 East View", city: "Austin", state: "TX", zipCode: "73344",
    guardianName: "Luis Hernandez", guardianPhone: "+1-512-555-2012", guardianEmail: "luis.h@email.com",
    govtIdType: "AADHAAR", govtIdNumber: "9012-3456-7890",
    enrollmentDate: new Date("2025-01-10"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(),
    rollNumber: "RC-E002", firstName: "James", lastName: "Moore",
    email: "james.moore@student.riverside.edu", phone: "+1-512-555-2013",
    dateOfBirth: new Date("2004-11-09"), gender: "Male",
    address: "222 Physics Lane", city: "Austin", state: "TX", zipCode: "73344",
    guardianName: "Sandra Moore", guardianPhone: "+1-512-555-2014", guardianEmail: "sandra.m@email.com",
    govtIdType: "DRIVING_LICENSE", govtIdNumber: "DL-TX-54321",
    enrollmentDate: new Date("2025-01-10"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Oakwood Central — Year 8 Alpha (2 students)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    rollNumber: "OW-001", firstName: "Isabella", lastName: "Anderson",
    email: "isabella.anderson@student.oakwood.edu", phone: "+1-503-555-3001",
    dateOfBirth: new Date("2012-06-18"), gender: "Female",
    address: "900 Oak Terrace", city: "Portland", state: "OR", zipCode: "97201",
    guardianName: "Thomas Anderson", guardianPhone: "+1-503-555-3002", guardianEmail: "thomas.a@email.com",
    govtIdType: "PASSPORT", govtIdNumber: "US-PASS-001",
    enrollmentDate: new Date("2025-09-01"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    rollNumber: "OW-002", firstName: "Lucas", lastName: "Thomas",
    email: "lucas.thomas@student.oakwood.edu", phone: "+1-503-555-3003",
    dateOfBirth: new Date("2012-02-27"), gender: "Male",
    address: "111 Forest Path", city: "Portland", state: "OR", zipCode: "97201",
    guardianName: "Patricia Thomas", guardianPhone: "+1-503-555-3004", guardianEmail: "patricia.t@email.com",
    govtIdType: "OTHER", govtIdNumber: "SSN-XXX-XX-1234",
    enrollmentDate: new Date("2025-09-01"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Oakwood Central — Year 9 Alpha (1 student — for graduation test)
  {
    arkId: "ARK-STU-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    rollNumber: "OW-003", firstName: "Mia", lastName: "Nakamura",
    email: "mia.nakamura@student.oakwood.edu", phone: "+1-503-555-3005",
    dateOfBirth: new Date("2011-04-10"), gender: "Female",
    address: "444 Sakura Lane", city: "Portland", state: "OR", zipCode: "97201",
    guardianName: "Hiroshi Nakamura", guardianPhone: "+1-503-555-3006", guardianEmail: "hiroshi.n@email.com",
    govtIdType: "PASSPORT", govtIdNumber: "JP-9876543",
    enrollmentDate: new Date("2024-09-01"), status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  }
]);

const studentIds = Object.values(students.insertedIds);
print(`Inserted ${studentIds.length} students`);

// =============================================================
// 5. Faculty (10 faculty with govtIdType/govtIdNumber, version)
// =============================================================
print("--- Inserting Faculty ---");

const facultyResult = db.faculty.insertMany([
  // Springfield Main Campus (3 faculty)
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    employeeId: "SA-FAC-001", firstName: "Dr. Margaret", lastName: "Chen",
    email: "margaret.chen@springfield.edu", phone: "+1-217-555-5001",
    dateOfBirth: new Date("1978-04-10"), gender: "Female",
    address: "200 Faculty Lane", city: "Springfield", state: "IL", zipCode: "62704",
    department: "Science", designation: "Head of Department",
    qualifications: ["Ph.D. Physics", "M.Sc. Physics"], specializations: ["Quantum Mechanics", "Thermodynamics"],
    joiningDate: new Date("2015-06-01"),
    govtIdType: "PAN", govtIdNumber: "AABCC1234D",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    employeeId: "SA-FAC-002", firstName: "James", lastName: "Patterson",
    email: "james.patterson@springfield.edu", phone: "+1-217-555-5002",
    dateOfBirth: new Date("1985-09-22"), gender: "Male",
    address: "305 Teacher Court", city: "Springfield", state: "IL", zipCode: "62704",
    department: "Mathematics", designation: "Senior Teacher",
    qualifications: ["M.Sc. Mathematics", "B.Ed."], specializations: ["Calculus", "Statistics"],
    joiningDate: new Date("2018-08-15"),
    govtIdType: "AADHAAR", govtIdNumber: "1111-2222-3333",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    employeeId: "SA-FAC-003", firstName: "Rachel", lastName: "Adams",
    email: "rachel.adams@springfield.edu", phone: "+1-217-555-5003",
    dateOfBirth: new Date("1992-03-18"), gender: "Female",
    address: "150 Academic Way", city: "Springfield", state: "IL", zipCode: "62704",
    department: "English", designation: "Teacher",
    qualifications: ["M.A. English", "B.Ed."], specializations: ["Grammar", "Literature"],
    joiningDate: new Date("2022-07-01"),
    govtIdType: "AADHAAR", govtIdNumber: "4444-5555-6666",
    status: "ON_LEAVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Springfield North Campus (2 faculty)
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(),
    employeeId: "SA-FAC-004", firstName: "Linda", lastName: "Thompson",
    email: "linda.thompson@springfield.edu", phone: "+1-217-555-5004",
    dateOfBirth: new Date("1990-01-15"), gender: "Female",
    address: "410 North Road", city: "Springfield", state: "IL", zipCode: "62707",
    department: "English", designation: "Teacher",
    qualifications: ["M.A. English Literature"], specializations: ["Creative Writing"],
    joiningDate: new Date("2021-07-01"),
    govtIdType: "PASSPORT", govtIdNumber: "US-PASS-FAC-001",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(),
    employeeId: "SA-FAC-005", firstName: "David", lastName: "Nguyen",
    email: "david.nguyen@springfield.edu", phone: "+1-217-555-5005",
    dateOfBirth: new Date("1987-11-25"), gender: "Male",
    address: "620 North Blvd", city: "Springfield", state: "IL", zipCode: "62707",
    department: "Science", designation: "Senior Teacher",
    qualifications: ["M.Sc. Chemistry"], specializations: ["Organic Chemistry"],
    joiningDate: new Date("2019-08-01"),
    govtIdType: "AADHAAR", govtIdNumber: "7777-8888-9999",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Riverside Downtown (2 faculty)
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    employeeId: "RC-FAC-001", firstName: "Dr. Sarah", lastName: "Mitchell",
    email: "sarah.mitchell@riverside.edu", phone: "+1-512-555-5001",
    dateOfBirth: new Date("1975-06-20"), gender: "Female",
    address: "100 University Ave", city: "Austin", state: "TX", zipCode: "73301",
    department: "Computer Science", designation: "Professor",
    qualifications: ["Ph.D. Computer Science", "M.S. CS"], specializations: ["Machine Learning", "Algorithms"],
    joiningDate: new Date("2010-01-15"),
    govtIdType: "DRIVING_LICENSE", govtIdNumber: "DL-TX-FAC-001",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    employeeId: "RC-FAC-002", firstName: "Prof. Robert", lastName: "Kumar",
    email: "robert.kumar@riverside.edu", phone: "+1-512-555-5002",
    dateOfBirth: new Date("1980-08-12"), gender: "Male",
    address: "200 Math Circle", city: "Austin", state: "TX", zipCode: "73301",
    department: "Mathematics", designation: "Associate Professor",
    qualifications: ["Ph.D. Mathematics"], specializations: ["Abstract Algebra", "Number Theory"],
    joiningDate: new Date("2014-08-01"),
    govtIdType: "PAN", govtIdNumber: "KLMNO5678P",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Riverside East (1 faculty)
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(),
    employeeId: "RC-FAC-003", firstName: "Dr. Emily", lastName: "Chen",
    email: "emily.chen@riverside.edu", phone: "+1-512-555-5003",
    dateOfBirth: new Date("1982-03-05"), gender: "Female",
    address: "300 East Science Park", city: "Austin", state: "TX", zipCode: "73344",
    department: "Physics", designation: "Professor",
    qualifications: ["Ph.D. Physics", "M.S. Applied Physics"], specializations: ["Optics", "Quantum Computing"],
    joiningDate: new Date("2012-01-10"),
    govtIdType: "AADHAAR", govtIdNumber: "1234-5678-9012",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  // Oakwood Central (2 faculty)
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    employeeId: "OW-FAC-001", firstName: "Patricia", lastName: "O'Brien",
    email: "patricia.obrien@oakwood.edu", phone: "+1-503-555-5001",
    dateOfBirth: new Date("1988-12-01"), gender: "Female",
    address: "500 Oak Faculty Row", city: "Portland", state: "OR", zipCode: "97201",
    department: "General Studies", designation: "Teacher",
    qualifications: ["M.Ed.", "B.A. Education"], specializations: ["Curriculum Design"],
    joiningDate: new Date("2020-08-01"),
    govtIdType: "VOTER_ID", govtIdNumber: "VOTER-OR-FAC001",
    status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now
  },
  {
    arkId: "ARK-FAC-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    employeeId: "OW-FAC-002", firstName: "Kenji", lastName: "Tanaka",
    email: "kenji.tanaka@oakwood.edu", phone: "+1-503-555-5002",
    dateOfBirth: new Date("1991-07-15"), gender: "Male",
    address: "600 Cedar Blvd", city: "Portland", state: "OR", zipCode: "97201",
    department: "Science", designation: "Teacher",
    qualifications: ["M.Sc. Biology"], specializations: ["Ecology", "Botany"],
    joiningDate: new Date("2023-01-15"),
    govtIdType: "PASSPORT", govtIdNumber: "JP-FAC-5678",
    status: "RESIGNED", version: NumberLong(0), createdAt: now, updatedAt: now
  }
]);

const facultyIds = Object.values(facultyResult.insertedIds);
print(`Inserted ${facultyIds.length} faculty`);

// =============================================================
// 6. Users (4-tier RBAC: SUPER_ADMIN, ORG_ADMIN, ADMIN, USER)
// =============================================================
print("--- Inserting Users ---");

db.users.insertMany([
  // SUPER_ADMIN (platform-wide, no org)
  {
    fullName: "Platform Super Admin", email: "superadmin@ark.platform",
    password: bcryptHash, role: "SUPER_ADMIN",
    organizationId: null, branchId: null, department: null,
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Springfield Academy
  {
    fullName: "Springfield Org Admin", email: "orgadmin@springfield.edu",
    password: bcryptHash, role: "ORG_ADMIN",
    organizationId: orgIds[0].toString(), branchId: null, department: null,
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Springfield Main Admin", email: "admin.main@springfield.edu",
    password: bcryptHash, role: "ADMIN",
    organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), department: "Academic Affairs",
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Springfield North Admin", email: "admin.north@springfield.edu",
    password: bcryptHash, role: "ADMIN",
    organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(), department: "Academic Affairs",
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Springfield Faculty User", email: "user.faculty@springfield.edu",
    password: bcryptHash, role: "USER",
    organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), department: "Science",
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Springfield Locked User", email: "locked@springfield.edu",
    password: bcryptHash, role: "USER",
    organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), department: "Admin Office",
    status: "LOCKED", createdAt: now, updatedAt: now
  },
  // Riverside College
  {
    fullName: "Riverside Org Admin", email: "orgadmin@riverside.edu",
    password: bcryptHash, role: "ORG_ADMIN",
    organizationId: orgIds[1].toString(), branchId: null, department: null,
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Riverside Downtown Admin", email: "admin.downtown@riverside.edu",
    password: bcryptHash, role: "ADMIN",
    organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), department: "Academics",
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Riverside User", email: "user@riverside.edu",
    password: bcryptHash, role: "USER",
    organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), department: "Student Services",
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Oakwood International
  {
    fullName: "Oakwood Org Admin", email: "orgadmin@oakwood.edu",
    password: bcryptHash, role: "ORG_ADMIN",
    organizationId: orgIds[2].toString(), branchId: null, department: null,
    status: "ACTIVE", createdAt: now, updatedAt: now
  },
  {
    fullName: "Oakwood Admin", email: "admin@oakwood.edu",
    password: bcryptHash, role: "ADMIN",
    organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), department: "Academic Affairs",
    status: "INACTIVE", createdAt: now, updatedAt: now
  }
]);

print("Inserted 11 users");

// =============================================================
// 7. Student Enrollments (active enrollments for all active students)
// =============================================================
print("--- Inserting Student Enrollments ---");

const enrollments = db.student_enrollments.insertMany([
  // Springfield Main Campus — 1st Class A: students 0,1,2 (student 3 is INACTIVE — no active enrollment)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[0].toString(), classId: classIds[0].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[1].toString(), classId: classIds[0].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[2].toString(), classId: classIds[0].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Springfield Main Campus — 2nd Class A: students 4,5
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[4].toString(), classId: classIds[1].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[5].toString(), classId: classIds[1].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Springfield Main Campus — 3rd Class A: student 6 (terminal for graduation test)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[6].toString(), classId: classIds[2].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Springfield Main Campus — prior year COMPLETED enrollment (student 4 was in 1st Class last year)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), studentId: studentIds[4].toString(), classId: classIds[3].toString(), academicYear: "2024-2025", enrolledAt: new Date("2024-06-20"), exitedAt: new Date("2025-05-30"), exitReason: "PROMOTED", status: "COMPLETED", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Springfield North Campus: students 7 (8 is TRANSFERRED — no active)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(), studentId: studentIds[7].toString(), classId: classIds[4].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-06-15"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Riverside Downtown — CS 101: students 9,10,11
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), studentId: studentIds[9].toString(), classId: classIds[5].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), studentId: studentIds[10].toString(), classId: classIds[5].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), studentId: studentIds[11].toString(), classId: classIds[5].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Riverside Downtown — Math 201: student 12 (13 is DROPPED)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), studentId: studentIds[12].toString(), classId: classIds[6].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Riverside East — Physics 101: students 14,15
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(), studentId: studentIds[14].toString(), classId: classIds[7].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(), studentId: studentIds[15].toString(), classId: classIds[7].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-01-10"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Oakwood Central — Year 8 Alpha: students 16,17
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), studentId: studentIds[16].toString(), classId: classIds[8].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-09-01"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), studentId: studentIds[17].toString(), classId: classIds[8].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-09-01"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now },
  // Oakwood Central — Year 9 Alpha: student 18 (terminal — graduation candidate)
  { arkId: "ARK-ENR-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), studentId: studentIds[18].toString(), classId: classIds[9].toString(), academicYear: "2025-2026", enrolledAt: new Date("2025-09-01"), exitedAt: null, exitReason: null, status: "ACTIVE", version: NumberLong(0), createdAt: now, updatedAt: now }
]);

const enrollmentIds = Object.values(enrollments.insertedIds);
print(`Inserted ${enrollmentIds.length} student enrollments`);

// =============================================================
// 8. Class Progressions (defines class ordering per branch)
// =============================================================
print("--- Inserting Class Progressions ---");

db.class_progressions.insertMany([
  // Springfield Main Campus: 1st → 2nd → 3rd (terminal)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[0].toString(),
    sequence: [
      { className: "1st Class", displayOrder: 1, isTerminal: false },
      { className: "2nd Class", displayOrder: 2, isTerminal: false },
      { className: "3rd Class", displayOrder: 3, isTerminal: true }
    ],
    createdAt: now,
    updatedAt: now
  },
  // Springfield North Campus: 1st → 2nd → 3rd (terminal)
  {
    organizationId: orgIds[0].toString(),
    branchId: branchIds[1].toString(),
    sequence: [
      { className: "1st Class", displayOrder: 1, isTerminal: false },
      { className: "2nd Class", displayOrder: 2, isTerminal: false },
      { className: "3rd Class", displayOrder: 3, isTerminal: true }
    ],
    createdAt: now,
    updatedAt: now
  },
  // Oakwood Central: Year 8 → Year 9 (terminal)
  {
    organizationId: orgIds[2].toString(),
    branchId: branchIds[4].toString(),
    sequence: [
      { className: "Year 8", displayOrder: 1, isTerminal: false },
      { className: "Year 9", displayOrder: 2, isTerminal: true }
    ],
    createdAt: now,
    updatedAt: now
  }
]);

print("Inserted 3 class progressions");

// =============================================================
// 9. Faculty Assignments (link faculty to classes/subjects)
// =============================================================
print("--- Inserting Faculty Assignments ---");

db.faculty_assignments.insertMany([
  // Springfield Main Campus
  // Dr. Margaret Chen → 1st Class A — Science (class teacher + subject)
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    facultyId: facultyIds[0].toString(), classId: classIds[0].toString(),
    subjectName: "Science", academicYear: "2025-2026",
    assignmentType: "BOTH", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // James Patterson → 1st Class A — Mathematics
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    facultyId: facultyIds[1].toString(), classId: classIds[0].toString(),
    subjectName: "Mathematics", academicYear: "2025-2026",
    assignmentType: "SUBJECT_TEACHER", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // James Patterson → 2nd Class A — Mathematics
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    facultyId: facultyIds[1].toString(), classId: classIds[1].toString(),
    subjectName: "Mathematics", academicYear: "2025-2026",
    assignmentType: "SUBJECT_TEACHER", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Rachel Adams → 2nd Class A — English (ON_LEAVE but assignment still active)
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    facultyId: facultyIds[2].toString(), classId: classIds[1].toString(),
    subjectName: "English", academicYear: "2025-2026",
    assignmentType: "CLASS_TEACHER", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Springfield North Campus
  // Linda Thompson → 1st Class A — English
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[1].toString(),
    facultyId: facultyIds[3].toString(), classId: classIds[4].toString(),
    subjectName: "English", academicYear: "2025-2026",
    assignmentType: "BOTH", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Riverside Downtown
  // Dr. Sarah Mitchell → CS 101 — Computer Science
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    facultyId: facultyIds[5].toString(), classId: classIds[5].toString(),
    subjectName: "Computer Science", academicYear: "2025-2026",
    assignmentType: "SUBJECT_TEACHER", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Prof. Robert Kumar → Math 201 — Mathematics
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    facultyId: facultyIds[6].toString(), classId: classIds[6].toString(),
    subjectName: "Mathematics", academicYear: "2025-2026",
    assignmentType: "SUBJECT_TEACHER", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Riverside East
  // Dr. Emily Chen → Physics 101 — Physics
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[3].toString(),
    facultyId: facultyIds[7].toString(), classId: classIds[7].toString(),
    subjectName: "Physics", academicYear: "2025-2026",
    assignmentType: "BOTH", status: "ACTIVE", createdAt: now, updatedAt: now
  },
  // Oakwood Central
  // Patricia O'Brien → Year 8 Alpha — General Studies
  {
    arkId: "ARK-ASGN-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    facultyId: facultyIds[8].toString(), classId: classIds[8].toString(),
    subjectName: "General Studies", academicYear: "2025-2026",
    assignmentType: "BOTH", status: "ACTIVE", createdAt: now, updatedAt: now
  }
]);

print("Inserted 9 faculty assignments");

// =============================================================
// 10. Examinations (FINAL and MIDTERM exams)
// =============================================================
print("--- Inserting Examinations ---");

const exams = db.examinations.insertMany([
  // Springfield Main Campus — Final Exam 2025-2026
  {
    arkId: "ARK-EXAM-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    name: "Final Examination 2025-2026", academicYear: "2025-2026", examType: "FINAL",
    startDate: new Date("2026-03-01"), endDate: new Date("2026-03-15"),
    description: "Annual final examination for all classes", status: "SCHEDULED",
    createdAt: now, updatedAt: now
  },
  // Springfield Main Campus — Midterm 2025-2026
  {
    arkId: "ARK-EXAM-" + nanoid(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(),
    name: "Midterm Examination 2025-2026", academicYear: "2025-2026", examType: "MIDTERM",
    startDate: new Date("2025-11-01"), endDate: new Date("2025-11-10"),
    description: "Midterm assessment", status: "COMPLETED",
    createdAt: now, updatedAt: now
  },
  // Riverside Downtown — Final Exam
  {
    arkId: "ARK-EXAM-" + nanoid(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(),
    name: "Spring Final 2025-2026", academicYear: "2025-2026", examType: "FINAL",
    startDate: new Date("2026-04-01"), endDate: new Date("2026-04-10"),
    description: "Spring semester final examination", status: "SCHEDULED",
    createdAt: now, updatedAt: now
  },
  // Oakwood Central — Final Exam
  {
    arkId: "ARK-EXAM-" + nanoid(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(),
    name: "Annual Examination 2025-2026", academicYear: "2025-2026", examType: "FINAL",
    startDate: new Date("2026-03-10"), endDate: new Date("2026-03-20"),
    description: "Annual examination for Year 8 and Year 9", status: "SCHEDULED",
    createdAt: now, updatedAt: now
  }
]);

const examIds = Object.values(exams.insertedIds);
print(`Inserted ${examIds.length} examinations`);

// =============================================================
// 11. Exam Subjects (subjects per exam per class)
// =============================================================
print("--- Inserting Exam Subjects ---");

const examSubjects = db.exam_subjects.insertMany([
  // Springfield Final Exam — 1st Class A subjects
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), subjectName: "Mathematics", subjectCode: "MATH-1A", maxMarks: 100, passingMarks: 35, examDate: new Date("2026-03-02"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), subjectName: "Science", subjectCode: "SCI-1A", maxMarks: 100, passingMarks: 35, examDate: new Date("2026-03-04"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), subjectName: "English", subjectCode: "ENG-1A", maxMarks: 100, passingMarks: 35, examDate: new Date("2026-03-06"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Springfield Final Exam — 2nd Class A subjects
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[1].toString(), subjectName: "Mathematics", subjectCode: "MATH-2A", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-03-02"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[1].toString(), subjectName: "English", subjectCode: "ENG-2A", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-03-06"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Springfield Final Exam — 3rd Class A subjects (terminal — graduation exam)
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[2].toString(), subjectName: "Mathematics", subjectCode: "MATH-3A", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-03-03"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  { examinationId: examIds[0].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[2].toString(), subjectName: "Science", subjectCode: "SCI-3A", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-03-05"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Springfield Midterm — 1st Class A (completed midterm with results)
  { examinationId: examIds[1].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), subjectName: "Mathematics", subjectCode: "MID-MATH-1A", maxMarks: 50, passingMarks: 18, examDate: new Date("2025-11-02"), status: "COMPLETED", createdAt: now, updatedAt: now },
  { examinationId: examIds[1].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), subjectName: "Science", subjectCode: "MID-SCI-1A", maxMarks: 50, passingMarks: 18, examDate: new Date("2025-11-04"), status: "COMPLETED", createdAt: now, updatedAt: now },
  // Riverside Final — CS 101 subjects
  { examinationId: examIds[2].toString(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), classId: classIds[5].toString(), subjectName: "Computer Science", subjectCode: "CS-101", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-04-02"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Riverside Final — Math 201 subjects
  { examinationId: examIds[2].toString(), organizationId: orgIds[1].toString(), branchId: branchIds[2].toString(), classId: classIds[6].toString(), subjectName: "Mathematics", subjectCode: "MATH-201", maxMarks: 100, passingMarks: 45, examDate: new Date("2026-04-04"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Oakwood Final — Year 8 Alpha subjects
  { examinationId: examIds[3].toString(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), classId: classIds[8].toString(), subjectName: "General Studies", subjectCode: "GS-Y8", maxMarks: 100, passingMarks: 35, examDate: new Date("2026-03-11"), status: "SCHEDULED", createdAt: now, updatedAt: now },
  // Oakwood Final — Year 9 Alpha subjects (terminal)
  { examinationId: examIds[3].toString(), organizationId: orgIds[2].toString(), branchId: branchIds[4].toString(), classId: classIds[9].toString(), subjectName: "General Studies", subjectCode: "GS-Y9", maxMarks: 100, passingMarks: 40, examDate: new Date("2026-03-12"), status: "SCHEDULED", createdAt: now, updatedAt: now }
]);

const subjectIds = Object.values(examSubjects.insertedIds);
print(`Inserted ${subjectIds.length} exam subjects`);

// =============================================================
// 12. Exam Results (midterm results for Springfield 1st Class A)
// =============================================================
print("--- Inserting Exam Results ---");

db.exam_results.insertMany([
  // Midterm — Mathematics (subject index 7) — students 0,1,2
  // Emma Johnson — 45/50 PASS
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[7].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[0].toString(), marksObtained: 45, grade: "A+", remarks: "Excellent performance", status: "PASS", createdAt: now, updatedAt: now },
  // Liam Williams — 38/50 PASS
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[7].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[1].toString(), marksObtained: 38, grade: "B+", remarks: "Good effort", status: "PASS", createdAt: now, updatedAt: now },
  // Aiden Clark — 15/50 FAIL
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[7].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[2].toString(), marksObtained: 15, grade: "F", remarks: "Needs improvement", status: "FAIL", createdAt: now, updatedAt: now },

  // Midterm — Science (subject index 8) — students 0,1,2
  // Emma Johnson — 42/50 PASS
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[8].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[0].toString(), marksObtained: 42, grade: "A", remarks: "Very good", status: "PASS", createdAt: now, updatedAt: now },
  // Liam Williams — 20/50 PASS (just passes)
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[8].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[1].toString(), marksObtained: 20, grade: "D", remarks: "Barely passed", status: "PASS", createdAt: now, updatedAt: now },
  // Aiden Clark — 10/50 FAIL
  { examinationId: examIds[1].toString(), examSubjectId: subjectIds[8].toString(), organizationId: orgIds[0].toString(), branchId: branchIds[0].toString(), classId: classIds[0].toString(), studentId: studentIds[2].toString(), marksObtained: 10, grade: "F", remarks: "Significant improvement needed", status: "FAIL", createdAt: now, updatedAt: now }
]);

print("Inserted 6 exam results");

// =============================================================
// 13. Indexes (ensure required compound indexes)
// =============================================================
print("--- Creating Indexes ---");

db.organizations.createIndex({ arkId: 1 }, { unique: true });
db.organizations.createIndex({ name: 1 }, { unique: true });
db.branches.createIndex({ arkId: 1 }, { unique: true });
db.branches.createIndex({ organizationId: 1, name: 1 }, { unique: true });
db.academic_classes.createIndex({ organizationId: 1, branchId: 1, name: 1, section: 1, academicYear: 1 }, { unique: true, name: "org_branch_class_name_section_year" });
db.students.createIndex({ arkId: 1 }, { unique: true });
db.faculty.createIndex({ arkId: 1 }, { unique: true });
db.users.createIndex({ organizationId: 1, email: 1 }, { unique: true, name: "org_email_idx" });
db.student_enrollments.createIndex({ studentId: 1, academicYear: 1 }, { unique: true, name: "student_year_unique" });
db.student_enrollments.createIndex({ organizationId: 1, classId: 1, status: 1 }, { name: "org_class_status" });
db.student_enrollments.createIndex({ studentId: 1, status: 1 }, { name: "student_status" });
db.student_enrollments.createIndex({ organizationId: 1, branchId: 1, academicYear: 1 }, { name: "org_branch_year" });
db.faculty_assignments.createIndex({ arkId: 1 }, { unique: true });
db.faculty_assignments.createIndex({ facultyId: 1, classId: 1, subjectName: 1, academicYear: 1 }, { unique: true, name: "faculty_class_subject_year_unique" });
db.exam_subjects.createIndex({ examinationId: 1, subjectName: 1, classId: 1 }, { unique: true, name: "exam_subject_unique" });
db.exam_results.createIndex({ examSubjectId: 1, studentId: 1 }, { unique: true, name: "result_unique" });
db.exam_results.createIndex({ examinationId: 1, studentId: 1 }, { name: "exam_student" });
db.examinations.createIndex({ arkId: 1 }, { unique: true });
db.examinations.createIndex({ organizationId: 1, branchId: 1, academicYear: 1, name: 1 }, { unique: true, name: "org_branch_year_name" });
db.class_progressions.createIndex({ organizationId: 1, branchId: 1 }, { unique: true, name: "org_branch_unique" });
db.audit_logs.createIndex({ organizationId: 1, timestamp: -1 }, { name: "org_timestamp_idx" });
db.audit_logs.createIndex({ timestamp: 1 }, { name: "timestamp_idx" });

print("Indexes created");

// =============================================================
// Summary
// =============================================================
print("\n========================================");
print("ARK Test Data Seeded Successfully (v3)");
print("========================================");
print(`Organizations: ${orgIds.length}`);
print(`Branches:      ${branchIds.length}`);
print(`Classes:       ${classIds.length}`);
print(`Students:      ${studentIds.length}`);
print(`Faculty:       ${facultyIds.length}`);
print("Users:         11");
print(`Enrollments:   ${enrollmentIds.length}`);
print("Progressions:  3");
print("Assignments:   9");
print(`Examinations:  ${examIds.length}`);
print(`Exam Subjects: ${subjectIds.length}`);
print("Exam Results:  6");
print("========================================");
print("\nTest Accounts (password: Password@123):");
print("  SUPER_ADMIN:  superadmin@ark.platform");
print("  ORG_ADMIN:    orgadmin@springfield.edu / orgadmin@riverside.edu / orgadmin@oakwood.edu");
print("  ADMIN:        admin.main@springfield.edu / admin.north@springfield.edu / admin.downtown@riverside.edu");
print("  USER:         user.faculty@springfield.edu / user@riverside.edu");
print("  LOCKED:       locked@springfield.edu (login should fail)");
print("  INACTIVE:     admin@oakwood.edu (login should fail)");
print("========================================");
print("\nPromotion Test Scenarios:");
print("  1. Promote 1st Class A → 2nd Class A (Springfield Main, 3 active students)");
print("  2. Graduate 3rd Class A (terminal class, 1 student — Sophia Martinez)");
print("  3. Promote Year 8 Alpha → Year 9 Alpha (Oakwood, 2 students)");
print("  4. Graduate Year 9 Alpha (terminal, 1 student — Mia Nakamura)");
print("========================================");
print("\nExam Result Test Scenarios:");
print("  Midterm results exist for Springfield 1st Class A (3 students, 2 subjects):");
print("    Emma Johnson:  Math 45/50 PASS, Science 42/50 PASS");
print("    Liam Williams: Math 38/50 PASS, Science 20/50 PASS");
print("    Aiden Clark:   Math 15/50 FAIL, Science 10/50 FAIL");
print("========================================");
