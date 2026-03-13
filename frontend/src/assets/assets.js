import logo from "./logo.png";
import menu from "./menu.png";
import menu1 from "./menu1.png";
import heart from "./heart.png";
import heart1 from "./heart1.png";
import plus from "./plus.png";
import plus1 from "./plus1.png";
import home from "./home.png";
import home1 from "./home1.png";
import search from "./search.png";
import search1 from "./search1.png";
import noProfilePic from "./noprofilepic.jpg";
import remove from "./remove.png";

export const assets = {
  logo,
  menu,
  menu1,
  heart,
  heart1,
  plus,
  plus1,
  home,
  home1,
  search,
  search1,
  noProfilePic,
  remove,
};

export const mockUsers = [
  {
    id: 1,
    name: "Марко Марковић",
    username: "marko123",
    bio: "Љубитељ фудбала и фотографије",
    profilePic: "https://randomuser.me/api/portraits/men/32.jpg",
    password: "sifraprvog",
    email: "markomarkovic@gmail.com",
    isPrivate: false,
    followers: [],
    following: [],
  },
  {
    id: 2,
    name: "Јелена Јовановић",
    username: "jelenaj",
    bio: "Књиге су моја страст",
    profilePic: "https://randomuser.me/api/portraits/women/44.jpg",
    password: "sifradrugog",
    email: "jelenajovanovic@gmail.com",
    isPrivate: false,
    followers: [],
    following: [],
  },
  {
    id: 3,
    name: "Никола Николић",
    username: "nikola_n",
    bio: "",
    profilePic: "https://randomuser.me/api/portraits/men/56.jpg",
    password: "sifratreceg",
    email: "nikolanikolic@gmail.com",
    isPrivate: false,
    followers: [],
    following: [],
  },
  {
    id: 4,
    name: "Марија Милић",
    username: "marijam",
    bio: "Волим путовања и авантуру",
    profilePic: "https://randomuser.me/api/portraits/women/65.jpg",
    password: "sifracetvrtog",
    email: "marijamilic@gmail.com",
    isPrivate: false,
    followers: [],
    following: [],
  },
  {
    id: 5,
    name: "Лука Лукић",
    username: "luka_l",
    bio: "Програмер у успону",
    profilePic: "",
    password: "sifrapetog",
    email: "lukalukic@gmail.com",
    isPrivate: false,
    followers: [],
    following: [],
  },
];

export const mockPosts = [
  {
    id: 1,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=1" }],
    description: "Moj prvi dan na planini!",
    createdAt: "2026-01-12T10:30:00",
  },
  {
    id: 2,
    authorId: 2,
    media: [
      { type: "image", url: "https://picsum.photos/400/300?random=2" },
      { type: "image", url: "https://picsum.photos/400/300?random=3" },
    ],
    description: "Danas smo se druzili u parku",
    createdAt: "2026-01-11T18:45:00",
  },
  // dodatne objave za authorId: 1
  {
    id: 3,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=4" }],
    description: "Prelep zalazak sunca večeras 🌅",
    createdAt: "2026-01-13T19:00:00",
  },
  {
    id: 4,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=5" }],
    description: "Uživam u šoljici kafe ☕",
    createdAt: "2026-01-14T09:15:00",
  },
  {
    id: 5,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=6" }],
    description: "Šetnja gradom u subotu 🏙️",
    createdAt: "2026-01-15T14:30:00",
  },
  {
    id: 6,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=7" }],
    description: "Vežbam na terasi 💪",
    createdAt: "2026-01-16T07:45:00",
  },
  {
    id: 7,
    authorId: 1,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=8" }],
    description: "Prva kafa u novom kafiću ☕❤️",
    createdAt: "2026-01-17T10:00:00",
  },
  // dodatne objave za authorId: 3
  {
    id: 8,
    authorId: 3,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=9" }],
    description: "Moja nova torba za posao 👜",
    createdAt: "2026-01-12T12:00:00",
  },
  {
    id: 9,
    authorId: 3,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=10" }],
    description: "Jutarnja trka u parku 🏃‍♀️",
    createdAt: "2026-01-13T06:30:00",
  },
  {
    id: 10,
    authorId: 3,
    media: [{ type: "image", url: "https://picsum.photos/400/300?random=11" }],
    description: "Veče uz knjigu 📖 ashas ads j asd a sdajh das",
    createdAt: "2026-01-14T20:15:00",
  },
];
