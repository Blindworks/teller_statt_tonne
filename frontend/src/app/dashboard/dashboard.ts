import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface Pickup {
  store: string;
  image: string;
  location: string;
  time: string;
  status: 'expiring' | 'standard';
}

interface NewsItem {
  type: 'event' | 'milestone' | 'maintenance';
  timeAgo: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  readonly pickups: Pickup[] = [
    {
      store: 'Bio-Markt Sonnenblume',
      image:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuAFdlvUh2maaucolzmM0IEm6P7OaO1Y5vMbi-7fklrk4_ts9JK8EybW3NtLMdh_ZYr8L_fBU_DFGUK9TGMvVFt9WMFjStAFX1h9k-PZa19qLxXJRDWGO1oj8clGPc3CghIM2oaNkshkkr-UVnX1f42bKhFzTVs3ljNPRaUbEAtKLzc2uZka2OUacWyAzaSRSP9StLkNyzn-Dauj8D08jmuQ7tTyH5zK77aBSLtkQ_m6zg9d2cWl78JWiOfb9D-hW6uZviwpxekplUiU',
      location: 'Kreuzberg Str. 12',
      time: '18:30 Today',
      status: 'expiring',
    },
    {
      store: 'Bäckerei Schmidt',
      image:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuBmeaEMwerRoADJITY1FS7PH3cHygjsou5NeqG_DuHG5vEpRvurSXHmNO8fd8Babx-n4qIO8cviaYCKTTG5FHg1GfnmhHhAzaZA1Gu3J_-zjfpXk3Hnj-IOSB_iMIERKQVIXpMDJQzO3FEDxaIa5eSM5jXkq5jNnU5vRH1NcUE3E8Jfd_Qh2MGPG7mQziuGSQ8Obw2BO7FS0Daip2RhGcP0FzzZSCeirrGTERE6_aXAZuj2_a4mRptqF73f_izqtwwdEeslTUJgLsLz',
      location: 'Hauptstraße 45',
      time: '20:00 Today',
      status: 'standard',
    },
    {
      store: 'Regio-Supermarkt',
      image:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCoJwFrlTuO6HjRyqhK32GGmLfAZ4hkVfm3ouk35R_2oJ74b7ckJOvWy_pZQySZJTxSbybmVfkPFJXTqIT4QnetM1so2lPh1dxo7jUjlA-jDV_nktIuGQlBMRuPqTlfGfxp0xmFUQ8iFlBxVQiG4rPYhq2NHXfJxqYa0TDbeWRzLE5PA4_OFlTrxKVsKKI54_w4pswhrXQC0RTmXPxxsxhKdfiofrBdiFGx7j1svFGyMZqZIzcW0-CkgbKBYGgSxyXnS6Fu4c2Rlr9W',
      location: 'Am Anger 2',
      time: '09:00 Tomorrow',
      status: 'standard',
    },
  ];

  readonly news: NewsItem[] = [
    {
      type: 'event',
      timeAgo: '2h ago',
      title: 'Summer Potluck at the Park next Sunday!',
      description: 'Bring your saved recipes and join us for a sunset feast at Park West.',
    },
    {
      type: 'milestone',
      timeAgo: '5h ago',
      title: "New Store: 'Veggie Bliss' joins the network",
      description: 'Our first dedicated vegan store is now available for pickups starting Monday.',
    },
    {
      type: 'maintenance',
      timeAgo: '1d ago',
      title: 'Updated Pickup Guidelines',
      description: 'Please read the new hygiene protocols for fresh produce handling.',
    },
  ];

  readonly activeAvatars = [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBj6LwGbCnoAgLSby3-xuN-scl2xdK_DTFQt9q8Drz_z8prWDKyT1JXVSxEqyyyzJDpxiW9JkNKltRKp3eGiXpq_WIoN2HHDH_9iOgxdm3BhLm3jP67pv0YnW2ymTptqItwj3xEByWSykcXg4InUHfIfxr38XTmy32GjifNC0xbPzUerRNqIFWof173glFhogMofDKcO9eXfeypu82yrJ63edO4d3oKWMJm8HyHOuDj6-bCbJGQIszfUQIektOC1ZQEu42COdRVwRwG',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuC_3J34nPEsvIbRVjvTvSDxgTa2IcrdzEjS7tNnTxPJDD5Scxn_dUaftO_gRjQ-hossMhGBgvp4nox5d7imb1SRLHWudLrr_MDljL6RAy9lvGp_k-naMIlv2QuRWJM_OZ9Dpq4ziMESnHfB7637e7hfARifhKh4Y9FDdv_LKCaCgy6smBckJLyog9N3bN4I15Y2DyS44f47RvJ33flmk-BEfZ97ctsVjfpz62RTbr-S5l5Dyq5mPyx7reYmJK8x-4MtzuTKFRmaXhRJ',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBZJaxHkkwsCXL9rBAbuiAcW4iWoQ114YgVUxRN4drIebkX-JSEhchHq1EskpN75EeV1bRWwyYGiKYTsz8ZRRVCqte7AXnuGaax7qZipzTiOmjDIZ40_gUsP3rRm5Pgc6Zgo5XJemn7fCHdr90wl91CLhamT0XbEcugTFsR6Hav8gCRVt81omhT7zkqbWdP86ns3gmJ1yUa447BEEKU83IkTMUNA_1LeZ2NFVLamHOpZ8jii4Toq0xzEzOdnY5FPMTugrji8ZyF_OjV',
  ];

  readonly profileImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAlnI7Jvlz_ItVX5RMs8c1rQ2KnMKp8akokDrB8ge2wAaZPKWb0ZDKUztGT9bQkumnREcvOTokVb7yTetcJwvZJIctkI5SOdI3iYH6EcWu-6h6KRX4XNypVJaFdgZglXJMWHELSUGH_u0Lvqx7Yy0AEwqDJ5KHcNMqF8eTxPtwdDcRpjpv75EulDc28zDPv0eIEFRMS9w0I8Yw0rbYWBF4HU9IFqaNxK5ICJ83u0UqpC-UhY4-fq1vwPvtT02JkgJhBJhKB8gWkHaQu';
}
