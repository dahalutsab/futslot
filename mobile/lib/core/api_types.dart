/// Shapes returned by the FutSlot API (backend: docs/api/openapi.yaml), as players see them. Money is in rupees.
library;

typedef Json = Map<String, dynamic>;

double _money(Object? value) => (value as num?)?.toDouble() ?? 0;

double? _moneyOrNull(Object? value) => (value as num?)?.toDouble();

List<T> _list<T>(Object? value, T Function(Json) from) =>
    (value as List<dynamic>? ?? const []).map((item) => from(item as Json)).toList();

/// An API enum value (LOSERS_PAY_ALL) as the Dart one (losersPayAll).
T _enum<T extends Enum>(List<T> values, Object? value) {
  final [first, ...rest] = (value as String).toLowerCase().split('_');
  return values.byName(first + rest.map((word) => word[0].toUpperCase() + word.substring(1)).join());
}

T? _enumOrNull<T extends Enum>(List<T> values, Object? value) => value == null ? null : _enum(values, value);

/// How the API spells an enum value: losersPayAll is LOSERS_PAY_ALL.
extension ApiName on Enum {
  String get apiName => name.replaceAllMapped(RegExp(r'[A-Z]|\d+'), (match) => '_${match[0]}').toUpperCase();
}

class User {
  const User({
    required this.id,
    required this.fullName,
    required this.phone,
    required this.email,
    required this.preferredLanguage,
    this.passwordSet = true,
    this.googleLinked = false,
  });

  final String id;
  final String fullName;
  final String phone;
  final String? email;
  final String preferredLanguage;

  /// False for accounts made before passwords, or with Google, until one is set. A session saved before the API
  /// said counts as having one.
  final bool passwordSet;
  final bool googleLinked;

  factory User.fromJson(Json json) => User(
        id: json['id'] as String,
        fullName: json['fullName'] as String,
        phone: json['phone'] as String,
        email: json['email'] as String?,
        preferredLanguage: json['preferredLanguage'] as String? ?? 'en',
        passwordSet: json['passwordSet'] as bool? ?? true,
        googleLinked: json['googleLinked'] as bool? ?? false,
      );

  Json toJson() => {
        'id': id,
        'fullName': fullName,
        'phone': phone,
        'email': email,
        'preferredLanguage': preferredLanguage,
        'passwordSet': passwordSet,
        'googleLinked': googleLinked,
      };
}

class TokenResponse {
  const TokenResponse({required this.accessToken, required this.refreshToken, required this.user});

  final String accessToken;
  final String refreshToken;
  final User user;

  factory TokenResponse.fromJson(Json json) => TokenResponse(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        user: User.fromJson(json['user'] as Json),
      );
}

/// Why a code is texted: proving the number for a new account or a Google link, or resetting a password.
enum CodePurpose { signUp, resetPassword, linkGoogle }

/// What sign-in offers: Google needs its web client id, and is off when that's null.
class AuthOptions {
  const AuthOptions({required this.googleWebClientId});

  final String? googleWebClientId;

  factory AuthOptions.fromJson(Json json) => AuthOptions(googleWebClientId: json['googleWebClientId'] as String?);
}

class OtpSent {
  const OtpSent({required this.expiresInSeconds, required this.resendAfterSeconds});

  final int expiresInSeconds;
  final int resendAfterSeconds;

  factory OtpSent.fromJson(Json json) => OtpSent(
        expiresInSeconds: json['expiresInSeconds'] as int,
        resendAfterSeconds: json['resendAfterSeconds'] as int,
      );
}

class Paged<T> {
  const Paged({required this.content, required this.page, required this.totalElements, required this.totalPages});

  final List<T> content;
  final int page;
  final int totalElements;
  final int totalPages;

  factory Paged.fromJson(Json json, T Function(Json) from) => Paged(
        content: _list(json['content'], from),
        page: json['page'] as int,
        totalElements: json['totalElements'] as int,
        totalPages: json['totalPages'] as int,
      );
}

// Venues

class VenueSummary {
  const VenueSummary({
    required this.id,
    required this.slug,
    required this.name,
    required this.area,
    required this.city,
    required this.averageRating,
    required this.reviewCount,
    required this.fromPrice,
    required this.courtCount,
    required this.coverPhotoUrl,
  });

  final String id;
  final String slug;
  final String name;
  final String? area;
  final String city;
  final double averageRating;
  final int reviewCount;
  final double? fromPrice;
  final int courtCount;
  final String? coverPhotoUrl;

  factory VenueSummary.fromJson(Json json) => VenueSummary(
        id: json['id'] as String,
        slug: json['slug'] as String,
        name: json['name'] as String,
        area: json['area'] as String?,
        city: json['city'] as String,
        averageRating: _money(json['averageRating']),
        reviewCount: json['reviewCount'] as int? ?? 0,
        fromPrice: _moneyOrNull(json['fromPrice']),
        courtCount: json['courtCount'] as int? ?? 0,
        coverPhotoUrl: json['coverPhotoUrl'] as String?,
      );
}

enum Weekday { monday, tuesday, wednesday, thursday, friday, saturday, sunday }

Weekday _weekday(String value) => Weekday.values.byName(value.toLowerCase());

/// Times are "HH:mm:ss"; a close at or before the open means open past midnight.
class DayHours {
  const DayHours({required this.day, required this.openTime, required this.closeTime, required this.closed});

  final Weekday day;
  final String? openTime;
  final String? closeTime;
  final bool closed;

  factory DayHours.fromJson(Json json) => DayHours(
        day: _weekday(json['day'] as String),
        openTime: json['openTime'] as String?,
        closeTime: json['closeTime'] as String?,
        closed: json['closed'] as bool? ?? false,
      );
}

/// Cancelling less than [withinHours] before the start costs [feePercent] of the price.
class CancellationTier {
  const CancellationTier({required this.withinHours, required this.feePercent});

  final int withinHours;
  final double feePercent;

  factory CancellationTier.fromJson(Json json) =>
      CancellationTier(withinHours: json['withinHours'] as int, feePercent: _money(json['feePercent']));
}

class VenuePolicy {
  const VenuePolicy({
    required this.advancePercent,
    required this.bookingHoldMinutes,
    required this.cancellationTiers,
    required this.maxAdvanceBookingDays,
    required this.strikeLimit,
    required this.strikeAdvancePercent,
    required this.depositRequired,
  });

  final double advancePercent;
  final int bookingHoldMinutes;

  /// Furthest from the start first; empty means cancelling is always free.
  final List<CancellationTier> cancellationTiers;
  final int maxAdvanceBookingDays;

  /// Late cancellations and no-shows in [strikeWindowDays] that raise the advance; null when off.
  final int? strikeLimit;
  final double? strikeAdvancePercent;

  /// Players need a full [depositAmount] FutSlot deposit to book here.
  final bool depositRequired;

  factory VenuePolicy.fromJson(Json json) => VenuePolicy(
        advancePercent: _money(json['advancePercent']),
        bookingHoldMinutes: json['bookingHoldMinutes'] as int,
        cancellationTiers: _list(json['cancellationTiers'], CancellationTier.fromJson),
        maxAdvanceBookingDays: json['maxAdvanceBookingDays'] as int,
        strikeLimit: json['strikeLimit'] as int?,
        strikeAdvancePercent: _moneyOrNull(json['strikeAdvancePercent']),
        depositRequired: json['depositRequired'] as bool? ?? false,
      );
}

/// How far back late cancellations and no-shows count as strikes.
const strikeWindowDays = 90;

/// The booking deposit players keep with FutSlot to book venues that require it.
const depositAmount = 1000.0;

class Amenity {
  const Amenity({required this.code, required this.name});

  final String code;
  final String name;

  factory Amenity.fromJson(Json json) => Amenity(code: json['code'] as String, name: json['name'] as String);
}

class Court {
  const Court({required this.id, required this.name});

  final String id;
  final String name;

  factory Court.fromJson(Json json) => Court(id: json['id'] as String, name: json['name'] as String);
}

class VenuePhoto {
  const VenuePhoto({required this.id, required this.url, required this.caption});

  final String id;
  final String url;
  final String? caption;

  factory VenuePhoto.fromJson(Json json) =>
      VenuePhoto(id: json['id'] as String, url: json['url'] as String, caption: json['caption'] as String?);
}

class VenueDetail {
  const VenueDetail({
    required this.id,
    required this.slug,
    required this.name,
    required this.description,
    required this.phone,
    required this.addressLine,
    required this.area,
    required this.city,
    required this.timezone,
    required this.policy,
    required this.paymentInstructions,
    required this.averageRating,
    required this.reviewCount,
    required this.operatingHours,
    required this.amenities,
    required this.courts,
    required this.photos,
  });

  final String id;
  final String slug;
  final String name;
  final String? description;
  final String? phone;
  final String addressLine;
  final String? area;
  final String city;
  final String timezone;
  final VenuePolicy policy;

  /// Where to pay the venue's advance: its own eSewa, Khalti or bank account.
  final String? paymentInstructions;
  final double averageRating;
  final int reviewCount;
  final List<DayHours> operatingHours;
  final List<Amenity> amenities;
  final List<Court> courts;
  final List<VenuePhoto> photos;

  String get address => '$addressLine${area != null ? ', $area' : ''}, $city';

  factory VenueDetail.fromJson(Json json) => VenueDetail(
        id: json['id'] as String,
        slug: json['slug'] as String,
        name: json['name'] as String,
        description: json['description'] as String?,
        phone: json['phone'] as String?,
        addressLine: json['addressLine'] as String,
        area: json['area'] as String?,
        city: json['city'] as String,
        timezone: json['timezone'] as String? ?? 'Asia/Kathmandu',
        policy: VenuePolicy.fromJson(json['policy'] as Json),
        paymentInstructions: json['paymentInstructions'] as String?,
        averageRating: _money(json['averageRating']),
        reviewCount: json['reviewCount'] as int? ?? 0,
        operatingHours: _list(json['operatingHours'], DayHours.fromJson),
        amenities: _list(json['amenities'], Amenity.fromJson),
        courts: _list(json['courts'], Court.fromJson),
        photos: _list(json['photos'], VenuePhoto.fromJson),
      );
}

class AddOnItem {
  const AddOnItem({required this.id, required this.name, required this.price, required this.active});

  final String id;
  final String name;
  final double price;
  final bool active;

  factory AddOnItem.fromJson(Json json) => AddOnItem(
        id: json['id'] as String,
        name: json['name'] as String,
        price: _money(json['price']),
        active: json['active'] as bool? ?? true,
      );
}

// Slots and bookings

enum SlotStatus { available, booked, blocked, past }

class Slot {
  const Slot({required this.startsAt, required this.endsAt, required this.price, required this.status});

  /// Venue-local, with offset, e.g. 2026-10-09T18:00:00+05:45.
  final String startsAt;
  final String endsAt;
  final double price;
  final SlotStatus status;

  factory Slot.fromJson(Json json) => Slot(
        startsAt: json['startsAt'] as String,
        endsAt: json['endsAt'] as String,
        price: _money(json['price']),
        status: SlotStatus.values.byName((json['status'] as String).toLowerCase()),
      );
}

class CourtSlots {
  const CourtSlots({required this.courtId, required this.name, required this.slots});

  final String courtId;
  final String name;
  final List<Slot> slots;

  factory CourtSlots.fromJson(Json json) => CourtSlots(
        courtId: json['courtId'] as String,
        name: json['name'] as String,
        slots: _list(json['slots'], Slot.fromJson),
      );
}

class Availability {
  const Availability({required this.date, required this.courts});

  final String date;
  final List<CourtSlots> courts;

  factory Availability.fromJson(Json json) =>
      Availability(date: json['date'] as String, courts: _list(json['courts'], CourtSlots.fromJson));
}

/// One slot, by its start from the availability response. A membership or a discount code, not both.
class BookingRequest {
  const BookingRequest({
    required this.courtId,
    required this.startsAt,
    this.notes,
    this.membershipId,
    this.discountCode,
    this.addOns = const {},
  });

  final String courtId;
  final String startsAt;
  final String? notes;
  final String? membershipId;
  final String? discountCode;

  /// Quantity by add-on id.
  final Map<String, int> addOns;

  BookingRequest withNotes(String? notes) => BookingRequest(
        courtId: courtId,
        startsAt: startsAt,
        notes: notes,
        membershipId: membershipId,
        discountCode: discountCode,
        addOns: addOns,
      );

  Json toJson() => {
        'courtId': courtId,
        'startsAt': startsAt,
        'notes': ?notes,
        'membershipId': ?membershipId,
        'discountCode': ?discountCode,
        'addOns': [
          for (final MapEntry(key: itemId, value: quantity) in addOns.entries)
            if (quantity > 0) {'itemId': itemId, 'quantity': quantity},
        ],
      };
}

/// `total = price + addOns - discount`; [advanceRequired] is what must be paid to confirm.
/// [strikeAdvance]: higher than usual because of recent late cancellations and no-shows.
/// [depositToAdd]: what the player must add to their FutSlot deposit before booking (0 when nothing).
class Quote {
  const Quote({
    required this.price,
    required this.addOns,
    required this.discount,
    required this.total,
    required this.advanceRequired,
    required this.strikeAdvance,
    required this.depositRequired,
    required this.depositToAdd,
    required this.discountCode,
  });

  final double price;
  final double addOns;
  final double discount;
  final double total;
  final double advanceRequired;
  final bool strikeAdvance;
  final bool depositRequired;
  final double depositToAdd;
  final String? discountCode;

  factory Quote.fromJson(Json json) => Quote(
        price: _money(json['price']),
        addOns: _money(json['addOns']),
        discount: _money(json['discount']),
        total: _money(json['total']),
        advanceRequired: _money(json['advanceRequired']),
        strikeAdvance: json['strikeAdvance'] as bool? ?? false,
        depositRequired: json['depositRequired'] as bool? ?? false,
        depositToAdd: _money(json['depositToAdd']),
        discountCode: json['discountCode'] as String?,
      );
}

enum BookingStatus { pendingPayment, confirmed, completed, cancelled, noShow, expired }

BookingStatus _bookingStatus(String value) => switch (value) {
      'PENDING_PAYMENT' => BookingStatus.pendingPayment,
      'CONFIRMED' => BookingStatus.confirmed,
      'COMPLETED' => BookingStatus.completed,
      'CANCELLED' => BookingStatus.cancelled,
      'NO_SHOW' => BookingStatus.noShow,
      _ => BookingStatus.expired,
    };

/// Something named by id, with its slug for links (and a venue's city where the API gives it).
class Ref {
  const Ref({required this.id, required this.name, this.slug, this.city});

  final String id;
  final String name;
  final String? slug;
  final String? city;

  factory Ref.fromJson(Json json) => Ref(
        id: json['id'] as String,
        name: json['name'] as String,
        slug: json['slug'] as String?,
        city: json['city'] as String?,
      );
}

class BookedAddOn {
  const BookedAddOn({required this.itemId, required this.name, required this.quantity, required this.unitPrice});

  final String itemId;
  final String name;
  final int quantity;
  final double unitPrice;

  factory BookedAddOn.fromJson(Json json) => BookedAddOn(
        itemId: json['itemId'] as String,
        name: json['name'] as String,
        quantity: json['quantity'] as int,
        unitPrice: _money(json['unitPrice']),
      );
}

class Booking {
  const Booking({
    required this.id,
    required this.reference,
    required this.venue,
    required this.court,
    required this.status,
    required this.startsAt,
    required this.endsAt,
    required this.priceAmount,
    required this.addOns,
    required this.discountAmount,
    required this.totalAmount,
    required this.advanceRequired,
    required this.paidAmount,
    required this.holdExpiresAt,
    required this.notes,
    required this.cancellationReason,
    required this.cancellationFeePercent,
    required this.depositBacked,
  });

  final String id;
  final String reference;
  final Ref venue;
  final Ref court;
  final BookingStatus status;
  final String startsAt;
  final String endsAt;
  final double priceAmount;
  final List<BookedAddOn> addOns;
  final double discountAmount;
  final double totalAmount;
  final double advanceRequired;
  final double paidAmount;
  final String? holdExpiresAt;
  final String? notes;
  final String? cancellationReason;

  /// The venue's fee share when the player cancelled late (or, if [depositBacked], didn't show up).
  final double cancellationFeePercent;

  /// Booked at a venue that requires the deposit, so fees the advance doesn't cover come out of it.
  final bool depositBacked;

  factory Booking.fromJson(Json json) => Booking(
        id: json['id'] as String,
        reference: json['reference'] as String,
        venue: Ref.fromJson(json['venue'] as Json),
        court: Ref.fromJson(json['court'] as Json),
        status: _bookingStatus(json['status'] as String),
        startsAt: json['startsAt'] as String,
        endsAt: json['endsAt'] as String,
        priceAmount: _money(json['priceAmount']),
        addOns: _list(json['addOns'], BookedAddOn.fromJson),
        discountAmount: _money(json['discountAmount']),
        totalAmount: _money(json['totalAmount']),
        advanceRequired: _money(json['advanceRequired']),
        paidAmount: _money(json['paidAmount']),
        holdExpiresAt: json['holdExpiresAt'] as String?,
        notes: json['notes'] as String?,
        cancellationReason: json['cancellationReason'] as String?,
        cancellationFeePercent: _money(json['cancellationFeePercent']),
        depositBacked: json['depositBacked'] as bool? ?? false,
      );
}

// Payments

enum PaymentMethod {
  cash('CASH'),
  esewa('ESEWA'),
  khalti('KHALTI'),
  fonepay('FONEPAY'),
  bankTransfer('BANK_TRANSFER');

  const PaymentMethod(this.api);

  /// How the API spells it.
  final String api;

  static PaymentMethod from(String value) =>
      PaymentMethod.values.firstWhere((method) => method.api == value, orElse: () => PaymentMethod.cash);
}

enum TransactionStatus { initiated, pending, succeeded, failed, cancelled, refunded, partiallyRefunded }

class Payment {
  const Payment({
    required this.id,
    required this.amount,
    required this.method,
    required this.status,
    required this.failureReason,
  });

  final String id;
  final double amount;
  final PaymentMethod method;
  final TransactionStatus status;
  final String? failureReason;

  factory Payment.fromJson(Json json) => Payment(
        id: json['id'] as String,
        amount: _money(json['amount']),
        method: PaymentMethod.from(json['method'] as String),
        status: switch (json['status'] as String) {
          'INITIATED' => TransactionStatus.initiated,
          'PENDING' => TransactionStatus.pending,
          'SUCCEEDED' => TransactionStatus.succeeded,
          'FAILED' => TransactionStatus.failed,
          'CANCELLED' => TransactionStatus.cancelled,
          'REFUNDED' => TransactionStatus.refunded,
          _ => TransactionStatus.partiallyRefunded,
        },
        failureReason: json['failureReason'] as String?,
      );
}

/// "I paid Rs X on eSewa, transaction Y".
class PaymentClaim {
  const PaymentClaim({required this.method, required this.transactionId, required this.amount});

  final PaymentMethod method;
  final String transactionId;
  final double amount;

  Json toJson() => {'method': method.api, 'transactionId': transactionId, 'amount': amount};
}

class Refund {
  const Refund({required this.id, required this.amount, required this.originalMethod, required this.sent});

  final String id;
  final double amount;
  final PaymentMethod originalMethod;

  /// The venue has paid it back.
  final bool sent;

  factory Refund.fromJson(Json json) => Refund(
        id: json['id'] as String,
        amount: _money(json['amount']),
        originalMethod: PaymentMethod.from(json['originalMethod'] as String),
        sent: json['status'] == 'SUCCEEDED',
      );
}

/// The money side of one booking, and where to pay the venue.
class BookingPayments {
  const BookingPayments({required this.paymentInstructions, required this.payments, required this.refunds});

  final String? paymentInstructions;
  final List<Payment> payments;
  final List<Refund> refunds;

  factory BookingPayments.fromJson(Json json) => BookingPayments(
        paymentInstructions: json['paymentInstructions'] as String?,
        payments: _list(json['payments'], Payment.fromJson),
        refunds: _list(json['refunds'], Refund.fromJson),
      );
}

// The FutSlot booking deposit

enum DepositEntryType { topUp, fee, withdrawal, withdrawalCancelled }

/// Positive money in, negative money out. [bookingReference] and [venueName] are set for fees.
class DepositEntry {
  const DepositEntry({
    required this.id,
    required this.type,
    required this.amount,
    required this.bookingReference,
    required this.venueName,
    required this.createdAt,
  });

  final String id;
  final DepositEntryType type;
  final double amount;
  final String? bookingReference;
  final String? venueName;
  final String createdAt;

  factory DepositEntry.fromJson(Json json) => DepositEntry(
        id: json['id'] as String,
        type: switch (json['type'] as String) {
          'TOP_UP' => DepositEntryType.topUp,
          'FEE' => DepositEntryType.fee,
          'WITHDRAWAL' => DepositEntryType.withdrawal,
          _ => DepositEntryType.withdrawalCancelled,
        },
        amount: _money(json['amount']),
        bookingReference: json['bookingReference'] as String?,
        venueName: json['venueName'] as String?,
        createdAt: json['createdAt'] as String,
      );
}

/// The whole balance on its way back; FutSlot sends it once [dueAt] has passed.
class DepositWithdrawal {
  const DepositWithdrawal({
    required this.id,
    required this.amount,
    required this.method,
    required this.sendTo,
    required this.dueAt,
  });

  final String id;
  final double amount;
  final PaymentMethod method;
  final String sendTo;
  final String dueAt;

  factory DepositWithdrawal.fromJson(Json json) => DepositWithdrawal(
        id: json['id'] as String,
        amount: _money(json['amount']),
        method: PaymentMethod.from(json['method'] as String),
        sendTo: json['sendTo'] as String,
        dueAt: json['dueAt'] as String,
      );
}

/// [toAdd] is what fills it (0 when full). [topUpBeingChecked]: a top-up was sent that FutSlot hasn't checked
/// yet; [paymentInstructions] says where to send one.
class Deposit {
  const Deposit({
    required this.required,
    required this.balance,
    required this.toAdd,
    required this.topUpBeingChecked,
    required this.paymentInstructions,
    required this.withdrawal,
    required this.history,
  });

  final double required;
  final double balance;
  final double toAdd;
  final bool topUpBeingChecked;
  final String? paymentInstructions;
  final DepositWithdrawal? withdrawal;
  final List<DepositEntry> history;

  factory Deposit.fromJson(Json json) => Deposit(
        required: _money(json['required']),
        balance: _money(json['balance']),
        toAdd: _money(json['toAdd']),
        topUpBeingChecked: json['topUpBeingChecked'] as bool? ?? false,
        paymentInstructions: json['paymentInstructions'] as String?,
        withdrawal: json['withdrawal'] == null ? null : DepositWithdrawal.fromJson(json['withdrawal'] as Json),
        history: _list(json['history'], DepositEntry.fromJson),
      );
}

// Reviews

enum ReviewStatus { published, hidden, removed }

class Review {
  const Review({
    required this.id,
    required this.venueName,
    required this.bookingId,
    required this.playedOn,
    required this.rating,
    required this.comment,
    required this.reviewerName,
    required this.ownerReply,
    required this.status,
  });

  final String id;
  final String venueName;
  final String? bookingId;
  final String playedOn;
  final int rating;
  final String? comment;
  final String reviewerName;
  final String? ownerReply;
  final ReviewStatus status;

  factory Review.fromJson(Json json) => Review(
        id: json['id'] as String,
        venueName: json['venueName'] as String? ?? '',
        bookingId: json['bookingId'] as String?,
        playedOn: json['playedOn'] as String,
        rating: json['rating'] as int,
        comment: json['comment'] as String?,
        reviewerName: json['reviewerName'] as String,
        ownerReply: json['ownerReply'] as String?,
        status: ReviewStatus.values.byName((json['status'] as String? ?? 'PUBLISHED').toLowerCase()),
      );
}

// Notifications

/// [data] holds ids to open what it's about: bookingId, conversationId, venueSlug...
class AppNotification {
  const AppNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.data,
    required this.read,
    required this.createdAt,
  });

  final String id;
  final String type;
  final String title;
  final String body;
  final Map<String, String> data;
  final bool read;
  final String createdAt;

  AppNotification markedRead() =>
      AppNotification(id: id, type: type, title: title, body: body, data: data, read: true, createdAt: createdAt);

  factory AppNotification.fromJson(Json json) => AppNotification(
        id: json['id'] as String,
        type: json['type'] as String,
        title: json['title'] as String,
        body: json['body'] as String,
        data: {
          for (final MapEntry(:key, :value) in (json['data'] as Map<String, dynamic>? ?? const {}).entries)
            if (value != null) key: value.toString(),
        },
        read: json['read'] as bool? ?? false,
        createdAt: json['createdAt'] as String,
      );
}

// Messages

/// One thread per player and venue.
class ConversationSummary {
  const ConversationSummary({
    required this.id,
    required this.venueId,
    required this.venueName,
    required this.venueSlug,
    required this.lastMessage,
    required this.lastMessageAt,
    required this.unread,
  });

  final String id;
  final String venueId;
  final String venueName;
  final String venueSlug;
  final String? lastMessage;
  final String? lastMessageAt;
  final int unread;

  ConversationSummary copyWith({String? lastMessage, String? lastMessageAt, int? unread}) => ConversationSummary(
        id: id,
        venueId: venueId,
        venueName: venueName,
        venueSlug: venueSlug,
        lastMessage: lastMessage ?? this.lastMessage,
        lastMessageAt: lastMessageAt ?? this.lastMessageAt,
        unread: unread ?? this.unread,
      );

  factory ConversationSummary.fromJson(Json json) => ConversationSummary(
        id: json['id'] as String,
        venueId: json['venueId'] as String,
        venueName: json['venueName'] as String,
        venueSlug: json['venueSlug'] as String,
        lastMessage: json['lastMessage'] as String?,
        lastMessageAt: json['lastMessageAt'] as String?,
        unread: json['unread'] as int? ?? 0,
      );
}

/// [fromYou]: sent by the player; venue replies are signed with the venue's name.
class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.senderName,
    required this.fromYou,
    required this.bookingId,
    required this.body,
    required this.readAt,
    required this.createdAt,
  });

  final String id;
  final String senderName;
  final bool fromYou;
  final String? bookingId;
  final String body;
  final String? readAt;
  final String createdAt;

  factory ChatMessage.fromJson(Json json) => ChatMessage(
        id: json['id'] as String,
        senderName: json['senderName'] as String,
        fromYou: json['fromYou'] as bool? ?? false,
        bookingId: json['bookingId'] as String?,
        body: json['body'] as String,
        readAt: json['readAt'] as String?,
        createdAt: json['createdAt'] as String,
      );
}

// Waitlist

enum WaitlistStatus { waiting, notified, booked, expired, cancelled }

/// A booked slot the player wants if it frees up; [WaitlistStatus.notified] means it did and they were told.
class WaitlistEntry {
  const WaitlistEntry({
    required this.id,
    required this.venueId,
    required this.venueName,
    required this.venueSlug,
    required this.courtId,
    required this.courtName,
    required this.startsAt,
    required this.endsAt,
    required this.status,
  });

  final String id;
  final String venueId;
  final String venueName;
  final String venueSlug;
  final String courtId;
  final String courtName;
  final String startsAt;
  final String endsAt;
  final WaitlistStatus status;

  /// Still waiting, or told it freed up: the slot can still be had.
  bool get live => status == WaitlistStatus.waiting || status == WaitlistStatus.notified;

  factory WaitlistEntry.fromJson(Json json) => WaitlistEntry(
        id: json['id'] as String,
        venueId: json['venueId'] as String,
        venueName: json['venueName'] as String,
        venueSlug: json['venueSlug'] as String,
        courtId: json['courtId'] as String,
        courtName: json['courtName'] as String,
        startsAt: json['startsAt'] as String,
        endsAt: json['endsAt'] as String,
        status: WaitlistStatus.values.byName((json['status'] as String).toLowerCase()),
      );
}

// Memberships

enum MembershipType { gamePack, monthlyPass }

enum MembershipStatus { pendingPayment, active, expired, cancelled }

/// A game pack ([gamesIncluded] games) or a pass ([discountPercent] off every game).
class MembershipPlan {
  const MembershipPlan({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.gamesIncluded,
    required this.discountPercent,
    required this.validityDays,
    required this.price,
    required this.active,
  });

  final String id;
  final String name;
  final String? description;
  final MembershipType type;
  final int? gamesIncluded;
  final double? discountPercent;
  final int validityDays;
  final double price;
  final bool active;

  factory MembershipPlan.fromJson(Json json) => MembershipPlan(
        id: json['id'] as String,
        name: json['name'] as String,
        description: json['description'] as String?,
        type: _enum(MembershipType.values, json['type']),
        gamesIncluded: json['gamesIncluded'] as int?,
        discountPercent: _moneyOrNull(json['discountPercent']),
        validityDays: json['validityDays'] as int,
        price: _money(json['price']),
        active: json['active'] as bool? ?? true,
      );
}

class Membership {
  const Membership({
    required this.id,
    required this.venue,
    required this.planName,
    required this.planType,
    required this.gamesIncluded,
    required this.discountPercent,
    required this.gamesRemaining,
    required this.status,
    required this.expiresOn,
  });

  final String id;
  final Ref venue;
  final String planName;
  final MembershipType planType;
  final int? gamesIncluded;
  final double? discountPercent;
  final int? gamesRemaining;
  final MembershipStatus status;

  /// YYYY-MM-DD.
  final String expiresOn;

  bool get active => status == MembershipStatus.active;

  factory Membership.fromJson(Json json) {
    final plan = json['plan'] as Json;
    return Membership(
      id: json['id'] as String,
      venue: Ref.fromJson(json['venue'] as Json),
      planName: plan['name'] as String,
      planType: _enum(MembershipType.values, plan['type']),
      gamesIncluded: plan['gamesIncluded'] as int?,
      discountPercent: _moneyOrNull(plan['discountPercent']),
      gamesRemaining: json['gamesRemaining'] as int?,
      status: _enum(MembershipStatus.values, json['status']),
      expiresOn: json['expiresOn'] as String,
    );
  }
}

// Splitting a booking's cost

enum SplitRule { equal, losersPayAll, losersPayPercent, losersPayCourt }

enum Side { a, b }

enum MatchResult { aWon, bWon, draw }

class SplitPerson {
  const SplitPerson({required this.userId, required this.name, required this.phone});

  final String userId;
  final String name;
  final String? phone;

  factory SplitPerson.fromJson(Json json) =>
      SplitPerson(userId: json['userId'] as String, name: json['name'] as String, phone: json['phone'] as String?);
}

/// [share] is set once the result is in; [phone] only for the booker.
class SplitPlayer {
  const SplitPlayer({
    required this.id,
    required this.name,
    required this.side,
    required this.booker,
    required this.you,
    required this.phone,
    required this.share,
    required this.settled,
  });

  final String id;
  final String name;
  final Side side;
  final bool booker;
  final bool you;
  final String? phone;
  final double? share;
  final bool settled;

  factory SplitPlayer.fromJson(Json json) => SplitPlayer(
        id: json['id'] as String,
        name: json['name'] as String,
        side: _enum(Side.values, json['side']),
        booker: json['booker'] as bool? ?? false,
        you: json['you'] as bool? ?? false,
        phone: json['phone'] as String?,
        share: _moneyOrNull(json['share']),
        settled: json['settled'] as bool? ?? false,
      );
}

/// What each player on a side pays for one result, roughly (shares differ by at most a rupee).
class SplitOutcome {
  const SplitOutcome({required this.result, required this.sideAEach, required this.sideBEach});

  final MatchResult result;
  final double sideAEach;
  final double sideBEach;

  factory SplitOutcome.fromJson(Json json) => SplitOutcome(
        result: _enum(MatchResult.values, json['result']),
        sideAEach: _money(json['sideAEach']),
        sideBEach: _money(json['sideBEach']),
      );
}

class CostSplit {
  const CostSplit({
    required this.bookingId,
    required this.venueName,
    required this.courtName,
    required this.startsAt,
    required this.bookedBy,
    required this.rule,
    required this.losersPercent,
    required this.courtAmount,
    required this.extrasAmount,
    required this.extrasNote,
    required this.total,
    required this.sideAName,
    required this.sideBName,
    required this.players,
    required this.result,
    required this.outcomes,
    required this.collected,
    required this.outstanding,
    required this.youAreBooker,
  });

  final String bookingId;
  final String venueName;
  final String courtName;
  final String startsAt;
  final SplitPerson bookedBy;
  final SplitRule rule;
  final int? losersPercent;
  final double courtAmount;
  final double extrasAmount;
  final String? extrasNote;
  final double total;
  final String sideAName;
  final String sideBName;
  final List<SplitPlayer> players;
  final MatchResult? result;
  final List<SplitOutcome> outcomes;
  final double collected;
  final double outstanding;
  final bool youAreBooker;

  String sideName(Side side) => side == Side.a ? sideAName : sideBName;

  factory CostSplit.fromJson(Json json) => CostSplit(
        bookingId: json['bookingId'] as String,
        venueName: json['venueName'] as String,
        courtName: json['courtName'] as String,
        startsAt: json['startsAt'] as String,
        bookedBy: SplitPerson.fromJson(json['bookedBy'] as Json),
        rule: _enum(SplitRule.values, json['rule']),
        losersPercent: (json['losersPercent'] as num?)?.toInt(),
        courtAmount: _money(json['courtAmount']),
        extrasAmount: _money(json['extrasAmount']),
        extrasNote: json['extrasNote'] as String?,
        total: _money(json['total']),
        sideAName: json['sideAName'] as String,
        sideBName: json['sideBName'] as String,
        players: _list(json['players'], SplitPlayer.fromJson),
        result: _enumOrNull(MatchResult.values, json['result']),
        outcomes: _list(json['outcomes'], SplitOutcome.fromJson),
        collected: _money(json['collected']),
        outstanding: _money(json['outstanding']),
        youAreBooker: json['youAreBooker'] as bool? ?? false,
      );
}

/// Money the player owes whoever booked a game they played in.
class OwedShare {
  const OwedShare({
    required this.bookingId,
    required this.venueName,
    required this.startsAt,
    required this.amount,
    required this.payTo,
  });

  final String bookingId;
  final String venueName;
  final String startsAt;
  final double amount;
  final SplitPerson payTo;

  factory OwedShare.fromJson(Json json) => OwedShare(
        bookingId: json['bookingId'] as String,
        venueName: json['venueName'] as String,
        startsAt: json['startsAt'] as String,
        amount: _money(json['amount']),
        payTo: SplitPerson.fromJson(json['payTo'] as Json),
      );
}

// Players and teams

enum SkillLevel { beginner, intermediate, advanced, pro }

enum Position { goalkeeper, defender, midfielder, forward, any }

enum PreferredFoot { left, right, both }

/// A player as others see them (short name); profile fields are null when not filled in or kept private.
class PlayerCard {
  const PlayerCard({
    required this.userId,
    required this.name,
    required this.position,
    required this.skillLevel,
    required this.preferredFoot,
    required this.city,
    required this.bio,
    required this.gamesPlayed,
    required this.isPublic,
  });

  final String userId;
  final String name;
  final Position? position;
  final SkillLevel? skillLevel;
  final PreferredFoot? preferredFoot;
  final String? city;
  final String? bio;
  final int gamesPlayed;

  /// Only on the player's own card: whether other players see the profile.
  final bool? isPublic;

  factory PlayerCard.fromJson(Json json) => PlayerCard(
        userId: json['userId'] as String,
        name: json['name'] as String,
        position: _enumOrNull(Position.values, json['position']),
        skillLevel: _enumOrNull(SkillLevel.values, json['skillLevel']),
        preferredFoot: _enumOrNull(PreferredFoot.values, json['preferredFoot']),
        city: json['city'] as String?,
        bio: json['bio'] as String?,
        gamesPlayed: json['gamesPlayed'] as int? ?? 0,
        isPublic: json['isPublic'] as bool?,
      );
}

enum TeamRole { captain, viceCaptain, member }

enum MemberStatus { invited, active, left, removed }

class TeamSummary {
  const TeamSummary({
    required this.id,
    required this.name,
    required this.slug,
    required this.city,
    required this.skillLevel,
    required this.memberCount,
  });

  final String id;
  final String name;
  final String slug;
  final String? city;
  final SkillLevel? skillLevel;
  final int memberCount;

  factory TeamSummary.fromJson(Json json) => TeamSummary(
        id: json['id'] as String,
        name: json['name'] as String,
        slug: json['slug'] as String,
        city: json['city'] as String?,
        skillLevel: _enumOrNull(SkillLevel.values, json['skillLevel']),
        memberCount: json['memberCount'] as int? ?? 0,
      );
}

class TeamMember {
  const TeamMember({required this.userId, required this.name, required this.role, required this.status});

  final String userId;
  final String name;
  final TeamRole role;
  final MemberStatus status;

  factory TeamMember.fromJson(Json json) => TeamMember(
        userId: json['userId'] as String,
        name: json['name'] as String,
        role: _enum(TeamRole.values, json['role']),
        status: _enum(MemberStatus.values, json['status']),
      );
}

/// Members see full names and their own role; leaders also see pending invites.
class Team {
  const Team({
    required this.id,
    required this.name,
    required this.slug,
    required this.city,
    required this.skillLevel,
    required this.memberCount,
    required this.members,
    required this.myRole,
    required this.myStatus,
  });

  final String id;
  final String name;
  final String slug;
  final String? city;
  final SkillLevel? skillLevel;
  final int memberCount;
  final List<TeamMember> members;
  final TeamRole? myRole;
  final MemberStatus? myStatus;

  bool get joined => myStatus == MemberStatus.active;

  /// The captain or vice-captain: invites players, enters tournaments, takes on opponents.
  bool get leads => joined && (myRole == TeamRole.captain || myRole == TeamRole.viceCaptain);

  factory Team.fromJson(Json json) => Team(
        id: json['id'] as String,
        name: json['name'] as String,
        slug: json['slug'] as String,
        city: json['city'] as String?,
        skillLevel: _enumOrNull(SkillLevel.values, json['skillLevel']),
        memberCount: json['memberCount'] as int? ?? 0,
        members: _list(json['members'], TeamMember.fromJson),
        myRole: _enumOrNull(TeamRole.values, json['myRole']),
        myStatus: _enumOrNull(MemberStatus.values, json['myStatus']),
      );
}

// Matchmaking

enum MatchPostType { needPlayers, findOpponent }

enum MatchPostStatus { open, filled, cancelled, expired }

enum JoinRequestStatus { pending, accepted, declined, withdrawn }

/// Someone asking to join a post; [phone] once they're accepted, and only for the poster.
class JoinRequest {
  const JoinRequest({
    required this.id,
    required this.player,
    required this.team,
    required this.message,
    required this.status,
    required this.phone,
  });

  final String id;
  final PlayerCard player;
  final Ref? team;
  final String? message;
  final JoinRequestStatus status;
  final String? phone;

  factory JoinRequest.fromJson(Json json) => JoinRequest(
        id: json['id'] as String,
        player: PlayerCard.fromJson(json['player'] as Json),
        team: json['team'] == null ? null : Ref.fromJson(json['team'] as Json),
        message: json['message'] as String?,
        status: _enum(JoinRequestStatus.values, json['status']),
        phone: json['phone'] as String?,
      );
}

/// Phones are shared once a request is accepted: the poster sees accepted players' numbers, accepted players see
/// the poster's ([contactPhone]). [requests] is only filled in for the poster.
class MatchPost {
  const MatchPost({
    required this.id,
    required this.type,
    required this.status,
    required this.postedByName,
    required this.team,
    required this.venue,
    required this.city,
    required this.startsAt,
    required this.playersNeeded,
    required this.placesTaken,
    required this.skillLevel,
    required this.costPerPlayer,
    required this.note,
    required this.mine,
    required this.myRequestStatus,
    required this.contactPhone,
    required this.requests,
  });

  final String id;
  final MatchPostType type;
  final MatchPostStatus status;
  final String postedByName;
  final Ref? team;
  final Ref? venue;
  final String? city;
  final String startsAt;
  final int? playersNeeded;
  final int placesTaken;
  final SkillLevel? skillLevel;
  final double? costPerPlayer;
  final String? note;
  final bool mine;

  /// Where the player's own request to join stands, if they made one.
  final JoinRequestStatus? myRequestStatus;
  final String? contactPhone;
  final List<JoinRequest> requests;

  factory MatchPost.fromJson(Json json) => MatchPost(
        id: json['id'] as String,
        type: _enum(MatchPostType.values, json['type']),
        status: _enum(MatchPostStatus.values, json['status']),
        postedByName: (json['postedBy'] as Json)['name'] as String,
        team: json['team'] == null ? null : Ref.fromJson(json['team'] as Json),
        venue: json['venue'] == null ? null : Ref.fromJson(json['venue'] as Json),
        city: json['city'] as String?,
        startsAt: json['startsAt'] as String,
        playersNeeded: json['playersNeeded'] as int?,
        placesTaken: json['placesTaken'] as int? ?? 0,
        skillLevel: _enumOrNull(SkillLevel.values, json['skillLevel']),
        costPerPlayer: _moneyOrNull(json['costPerPlayer']),
        note: json['note'] as String?,
        mine: json['mine'] as bool? ?? false,
        myRequestStatus: _enumOrNull(JoinRequestStatus.values, (json['myRequest'] as Json?)?['status']),
        contactPhone: json['contactPhone'] as String?,
        requests: _list(json['requests'], JoinRequest.fromJson),
      );
}

// Tournaments

enum TournamentFormat { knockout, league, groupKnockout }

enum TournamentStatus { draft, registrationOpen, registrationClosed, inProgress, completed, cancelled }

/// A round of fixtures; spelled out, since `final` can't name a Dart enum value.
enum Stage {
  group('GROUP'),
  league('LEAGUE'),
  roundOf32('ROUND_OF_32'),
  roundOf16('ROUND_OF_16'),
  quarterFinal('QUARTER_FINAL'),
  semiFinal('SEMI_FINAL'),
  thirdPlace('THIRD_PLACE'),
  finalMatch('FINAL');

  const Stage(this.api);

  /// How the API spells it.
  final String api;

  static Stage from(String value) => Stage.values.firstWhere((stage) => stage.api == value, orElse: () => Stage.league);
}

enum EntryStatus { pendingPayment, confirmed, waitlisted, withdrawn, rejected }

enum FixtureStatus { scheduled, inProgress, completed, walkover, postponed, cancelled }

class EntryTeam {
  const EntryTeam({required this.registrationId, required this.teamId, required this.name});

  final String registrationId;
  final String teamId;
  final String name;

  factory EntryTeam.fromJson(Json json) => EntryTeam(
        registrationId: json['registrationId'] as String,
        teamId: json['teamId'] as String,
        name: json['name'] as String,
      );
}

class TournamentSummary {
  const TournamentSummary({
    required this.id,
    required this.slug,
    required this.name,
    required this.venue,
    required this.format,
    required this.status,
    required this.startsOn,
    required this.endsOn,
    required this.entryFee,
    required this.teamsIn,
    required this.maxTeams,
    required this.bannerUrl,
  });

  final String id;
  final String slug;
  final String name;
  final Ref venue;
  final TournamentFormat format;
  final TournamentStatus status;
  final String? bannerUrl;

  /// YYYY-MM-DD.
  final String startsOn;
  final String endsOn;
  final double entryFee;
  final int teamsIn;
  final int maxTeams;

  factory TournamentSummary.fromJson(Json json) => TournamentSummary(
        id: json['id'] as String,
        slug: json['slug'] as String,
        name: json['name'] as String,
        venue: Ref.fromJson(json['venue'] as Json),
        format: _enum(TournamentFormat.values, json['format']),
        status: _enum(TournamentStatus.values, json['status']),
        startsOn: json['startsOn'] as String,
        endsOn: json['endsOn'] as String,
        entryFee: _money(json['entryFee']),
        teamsIn: json['teamsIn'] as int? ?? 0,
        maxTeams: json['maxTeams'] as int? ?? 0,
        bannerUrl: json['bannerUrl'] as String?,
      );
}

class SquadPlayer {
  const SquadPlayer({required this.userId, required this.name, required this.jerseyNumber});

  final String userId;
  final String name;
  final int? jerseyNumber;

  factory SquadPlayer.fromJson(Json json) => SquadPlayer(
        userId: json['userId'] as String,
        name: json['name'] as String,
        jerseyNumber: json['jerseyNumber'] as int?,
      );
}

class TournamentEntry {
  const TournamentEntry({
    required this.id,
    required this.team,
    required this.status,
    required this.groupName,
    required this.squad,
  });

  final String id;
  final EntryTeam team;
  final EntryStatus status;
  final String? groupName;
  final List<SquadPlayer> squad;

  factory TournamentEntry.fromJson(Json json) => TournamentEntry(
        id: json['id'] as String,
        team: EntryTeam.fromJson(json['team'] as Json),
        status: _enum(EntryStatus.values, json['status']),
        groupName: json['groupName'] as String?,
        squad: _list(json['squad'], SquadPlayer.fromJson),
      );
}

class Fixture {
  const Fixture({
    required this.id,
    required this.stage,
    required this.groupName,
    required this.home,
    required this.away,
    required this.scheduledAt,
    required this.homeScore,
    required this.awayScore,
    required this.homePenalties,
    required this.awayPenalties,
    required this.winnerRegistrationId,
    required this.status,
    required this.bye,
  });

  final String id;
  final Stage stage;
  final String? groupName;
  final EntryTeam? home;
  final EntryTeam? away;

  /// Venue-local with offset.
  final String? scheduledAt;
  final int? homeScore;
  final int? awayScore;
  final int? homePenalties;
  final int? awayPenalties;
  final String? winnerRegistrationId;
  final FixtureStatus status;
  final bool bye;

  factory Fixture.fromJson(Json json) => Fixture(
        id: json['id'] as String,
        stage: Stage.from(json['stage'] as String),
        groupName: json['groupName'] as String?,
        home: json['home'] == null ? null : EntryTeam.fromJson(json['home'] as Json),
        away: json['away'] == null ? null : EntryTeam.fromJson(json['away'] as Json),
        scheduledAt: json['scheduledAt'] as String?,
        homeScore: json['homeScore'] as int?,
        awayScore: json['awayScore'] as int?,
        homePenalties: json['homePenalties'] as int?,
        awayPenalties: json['awayPenalties'] as int?,
        winnerRegistrationId: json['winnerRegistrationId'] as String?,
        status: _enum(FixtureStatus.values, json['status']),
        bye: json['bye'] as bool? ?? false,
      );
}

class Standing {
  const Standing({
    required this.team,
    required this.played,
    required this.won,
    required this.drawn,
    required this.lost,
    required this.goalDifference,
    required this.points,
  });

  final EntryTeam team;
  final int played;
  final int won;
  final int drawn;
  final int lost;
  final int goalDifference;
  final int points;

  factory Standing.fromJson(Json json) => Standing(
        team: EntryTeam.fromJson(json['team'] as Json),
        played: json['played'] as int,
        won: json['won'] as int,
        drawn: json['drawn'] as int,
        lost: json['lost'] as int,
        goalDifference: json['goalDifference'] as int,
        points: json['points'] as int,
      );
}

/// A league table, or one group's.
class StandingsTable {
  const StandingsTable({required this.groupName, required this.rows});

  final String? groupName;
  final List<Standing> rows;

  factory StandingsTable.fromJson(Json json) =>
      StandingsTable(groupName: json['groupName'] as String?, rows: _list(json['rows'], Standing.fromJson));
}

class TournamentDetail {
  const TournamentDetail({
    required this.id,
    required this.slug,
    required this.name,
    required this.venue,
    required this.description,
    required this.rules,
    required this.bannerUrl,
    required this.format,
    required this.status,
    required this.teamSize,
    required this.maxSquadSize,
    required this.maxTeams,
    required this.entryFee,
    required this.prizeDetails,
    required this.registrationClosesAt,
    required this.startsOn,
    required this.endsOn,
    required this.acceptingTeams,
    required this.teamsIn,
    required this.registrations,
    required this.matches,
    required this.tables,
    required this.champion,
  });

  final String id;
  final String slug;
  final String name;
  final Ref venue;
  final String? description;
  final String? rules;
  final String? bannerUrl;
  final TournamentFormat format;
  final TournamentStatus status;
  final int teamSize;
  final int maxSquadSize;
  final int maxTeams;
  final double entryFee;
  final String? prizeDetails;
  final String? registrationClosesAt;
  final String startsOn;
  final String endsOn;
  final bool acceptingTeams;
  final int teamsIn;
  final List<TournamentEntry> registrations;
  final List<Fixture> matches;
  final List<StandingsTable> tables;
  final EntryTeam? champion;

  factory TournamentDetail.fromJson(Json json) => TournamentDetail(
        id: json['id'] as String,
        slug: json['slug'] as String,
        name: json['name'] as String,
        venue: Ref.fromJson(json['venue'] as Json),
        description: json['description'] as String?,
        rules: json['rules'] as String?,
        bannerUrl: json['bannerUrl'] as String?,
        format: _enum(TournamentFormat.values, json['format']),
        status: _enum(TournamentStatus.values, json['status']),
        teamSize: json['teamSize'] as int,
        maxSquadSize: json['maxSquadSize'] as int,
        maxTeams: json['maxTeams'] as int,
        entryFee: _money(json['entryFee']),
        prizeDetails: json['prizeDetails'] as String?,
        registrationClosesAt: json['registrationClosesAt'] as String?,
        startsOn: json['startsOn'] as String,
        endsOn: json['endsOn'] as String,
        acceptingTeams: json['acceptingTeams'] as bool? ?? false,
        teamsIn: json['teamsIn'] as int? ?? 0,
        registrations: _list(json['registrations'], TournamentEntry.fromJson),
        matches: _list(json['matches'], Fixture.fromJson),
        tables: _list(json['tables'], StandingsTable.fromJson),
        champion: json['champion'] == null ? null : EntryTeam.fromJson(json['champion'] as Json),
      );
}

/// An entry of one of the player's teams, with the tournament it's in.
class MyTournamentEntry {
  const MyTournamentEntry({required this.tournament, required this.entry});

  final TournamentSummary tournament;
  final TournamentEntry entry;

  factory MyTournamentEntry.fromJson(Json json) => MyTournamentEntry(
        tournament: TournamentSummary.fromJson(json['tournament'] as Json),
        entry: TournamentEntry.fromJson(json['entry'] as Json),
      );
}
